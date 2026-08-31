package com.perfflow.module.deptassessment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.deptassessment.dto.DeptActualValueReq;
import com.perfflow.module.deptassessment.dto.DeptActualValueRowReq;
import com.perfflow.module.deptassessment.dto.DeptAssessmentOptionResp;
import com.perfflow.module.deptassessment.dto.DeptAssessmentResp;
import com.perfflow.module.deptassessment.dto.DeptKpiRowResp;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import com.perfflow.module.deptassessment.entity.DeptKpiRow;
import com.perfflow.module.deptassessment.enums.DeptAssessmentState;
import com.perfflow.module.deptassessment.mapper.DeptAssessmentMapper;
import com.perfflow.module.deptassessment.mapper.DeptKpiRowMapper;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.enums.PeriodType;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 部门考核服务：KPI 填报、复核、初审、审批、部门等级自动计算。
 *
 * <p>状态流转采用"原子条件更新 + updated==0 抛错"范式，防止并发重复流转。
 * <p>填报仅允许绩效专员维护"实际完成值"；得分由系统按完成率自动计算，不对外编辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptAssessmentService {

    /** KPI 行类型 */
    public static final String ROW_TYPE_KPI = "经营业绩";
    public static final String ROW_TYPE_OPERATION = "运营指标";
    public static final String ROW_TYPE_KEY_WORK = "重点工作";

    /** 每类 KPI 行数量 */
    private static final int KPI_ROW_COUNT = 3;
    private static final int OPERATION_ROW_COUNT = 2;
    private static final int KEY_WORK_ROW_COUNT = 2;

    /** 周期状态：进行中（HR 已开启） */
    private static final int PERIOD_STATUS_OPEN = 1;

    /** 完成率上限（超额完成按 100% 计） */
    private static final BigDecimal RATIO_CAP = BigDecimal.ONE;

    /** 完成率除法保留小数位数 */
    private static final int RATIO_SCALE = 4;

    /** 得分保留小数位数 */
    private static final int SCORE_SCALE = 2;

    private final DeptAssessmentMapper deptAssessmentMapper;
    private final DeptKpiRowMapper kpiRowMapper;
    private final AssessmentPeriodMapper periodMapper;
    private final SysDepartmentMapper deptMapper;

    private final DeptAssessmentStateMachine stateMachine;

    /**
     * 部门类周期开启时，为勾选部门（或全部部门）生成部门考核主表 + KPI 行模板。
     *
     * <p>已存在记录跳过（幂等），不覆盖已有 KPI 填报数据。
     *
     * @param period  部门类周期
     * @param deptIds 参与部门 ID 列表，为空表示全部部门
     */
    @Transactional(rollbackFor = Exception.class)
    public void initForPeriod(AssessmentPeriod period, List<Long> deptIds) {
        QueryWrapper<SysDepartment> qw = new QueryWrapper<>();
        if (deptIds != null && !deptIds.isEmpty()) {
            qw.in("id", deptIds);
        }
        qw.orderByAsc("id");
        List<SysDepartment> depts = deptMapper.selectList(qw);
        for (SysDepartment d : depts) {
            if (d.getId() == null) {
                continue;
            }
            if (findAssessment(period.getId(), d.getId()) != null) {
                continue;
            }
            initAssessment(period.getId(), d.getId());
        }
        log.info("部门类周期开启初始化完成: periodId={}, 部门数={}, 生成主表数={}",
                period.getId(), depts.size(), depts.size());
    }

    /**
     * 查询本部门可填报的部门考核选项（仅 HR 已开启的部门线周期）。
     *
     * <p>HR 未开启部门考核、或本部门未被勾选参与时返回空列表，前端展示"暂未开启部门填报"。
     *
     * @param deptId 部门ID
     * @return 填报选项列表（按周期年/季度倒序）
     */
    public List<DeptAssessmentOptionResp> listOptions(Long deptId) {
        List<AssessmentPeriod> periods = periodMapper.selectList(new QueryWrapper<AssessmentPeriod>()
                .eq("status", PERIOD_STATUS_OPEN)
                .orderByDesc("year")
                .orderByDesc("quarter"));
        List<DeptAssessmentOptionResp> out = new ArrayList<>(periods.size());
        for (AssessmentPeriod p : periods) {
            if (!isDeptLinePeriod(p)) {
                continue;
            }
            DeptAssessment assessment = findAssessment(p.getId(), deptId);
            if (assessment == null) {
                continue;
            }
            out.add(toOptionResp(p, assessment));
        }
        return out;
    }

    /**
     * 按主表ID查询部门考核详情（含 KPI 行）。
     *
     * <p>数据范围：部门绩效专员/部门负责人仅可见本部门；运营/委员会/绩效管理员可见全部。
     *
     * @param id 部门考核主表ID
     * @return 部门考核详情
     */
    public DeptAssessmentResp getById(Long id) {
        DeptAssessment assessment = requiredById(id);
        assertViewPermission(assessment.getDeptId());
        return toResp(assessment);
    }

    /**
     * 部门绩效专员提交本部门 KPI 实际完成值（自评中 → 待复核）。
     *
     * <p>仅允许修改 actualValue，其余字段（指标/目标值/评分标准/权重/得分）不可由专员维护；
     * 得分在提交时由系统按完成率自动重算。
     *
     * @param assessmentId 部门考核主表ID
     * @param req          填报请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long assessmentId, DeptActualValueReq req) {
        DeptAssessment assessment = requiredById(assessmentId);
        assertDeptStaff(assessment.getDeptId());
        requireOpenPeriod(assessment.getPeriodId());
        stateMachine.assertInState(assessment, DeptAssessmentState.SELF_FILLING);

        // 仅保存实际完成值，得分自动计算
        saveActualValues(assessment.getId(), req.getRows());
        recalcScores(assessment.getId());

        int updated = deptAssessmentMapper.update(null, new LambdaUpdateWrapper<DeptAssessment>()
                .eq(DeptAssessment::getId, assessment.getId())
                .eq(DeptAssessment::getStatus, DeptAssessmentState.SELF_FILLING.getCode())
                .set(DeptAssessment::getStatus, DeptAssessmentState.PENDING_REVIEW.getCode())
                .set(DeptAssessment::getSubmittedAt, LocalDateTime.now())
                .set(DeptAssessment::getVersion, (assessment.getVersion() == null ? 0 : assessment.getVersion()) + 1));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "部门考核状态已变化，请刷新后重试");
        }
        log.info("部门绩效专员提交部门考核: assessmentId={}, deptId={}", assessmentId, assessment.getDeptId());
    }

    /**
     * 部门负责人复核（待复核 → 待初审 / 退回自评中）。
     *
     * @param assessmentId 部门考核主表ID
     * @param approve      是否通过
     * @param comment      意见/原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void review(Long assessmentId, boolean approve, String comment) {
        DeptAssessment assessment = requiredById(assessmentId);
        assertDeptLead(assessment.getDeptId());
        requireOpenPeriod(assessment.getPeriodId());
        stateMachine.assertInState(assessment, DeptAssessmentState.PENDING_REVIEW);

        if (!approve) {
            rollbackToFilling(assessment, comment, "复核退回");
            return;
        }
        int updated = deptAssessmentMapper.update(null, new LambdaUpdateWrapper<DeptAssessment>()
                .eq(DeptAssessment::getId, assessment.getId())
                .eq(DeptAssessment::getStatus, DeptAssessmentState.PENDING_REVIEW.getCode())
                .set(DeptAssessment::getStatus, DeptAssessmentState.PENDING_AUDIT.getCode())
                .set(DeptAssessment::getReviewedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "部门考核状态已变化，请刷新后重试");
        }
        log.info("部门负责人复核通过: assessmentId={}, deptId={}", assessmentId, assessment.getDeptId());
    }

    /**
     * 运营管理部初审（待初审 → 待审批 / 退回整改）。
     *
     * @param assessmentId 部门考核主表ID
     * @param approve      是否通过
     * @param comment      意见/原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long assessmentId, boolean approve, String comment) {
        assertOperation();
        DeptAssessment assessment = requiredById(assessmentId);
        requireOpenPeriod(assessment.getPeriodId());
        stateMachine.assertInState(assessment, DeptAssessmentState.PENDING_AUDIT);

        if (!approve) {
            rollbackToFilling(assessment, comment, "初审退回整改");
            return;
        }
        int updated = deptAssessmentMapper.update(null, new LambdaUpdateWrapper<DeptAssessment>()
                .eq(DeptAssessment::getId, assessment.getId())
                .eq(DeptAssessment::getStatus, DeptAssessmentState.PENDING_AUDIT.getCode())
                .set(DeptAssessment::getStatus, DeptAssessmentState.PENDING_APPROVE.getCode()));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "部门考核状态已变化，请刷新后重试");
        }
        log.info("运营管理部初审通过: assessmentId={}, deptId={}", assessmentId, assessment.getDeptId());
    }

    /**
     * 绩效委员会最终审批（待审批 → 已完成，触发部门等级计算）。
     *
     * @param assessmentId 部门考核主表ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long assessmentId) {
        assertCommittee();
        DeptAssessment assessment = requiredById(assessmentId);
        requireOpenPeriod(assessment.getPeriodId());
        stateMachine.assertInState(assessment, DeptAssessmentState.PENDING_APPROVE);

        // 审批前重算得分，确保与最新实际完成值一致
        recalcScores(assessment.getId());
        BigDecimal total = calcTotalScore(assessment.getId());
        String grade = gradeOf(total);

        int updated = deptAssessmentMapper.update(null, new LambdaUpdateWrapper<DeptAssessment>()
                .eq(DeptAssessment::getId, assessment.getId())
                .eq(DeptAssessment::getStatus, DeptAssessmentState.PENDING_APPROVE.getCode())
                .set(DeptAssessment::getStatus, DeptAssessmentState.COMPLETED.getCode())
                .set(DeptAssessment::getTotalScore, total)
                .set(DeptAssessment::getDeptGrade, grade)
                .set(DeptAssessment::getApprovedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "部门考核状态已变化，请刷新后重试");
        }
        log.info("绩效委员会审批完成: assessmentId={}, deptId={}, totalScore={}, grade={}",
                assessmentId, assessment.getDeptId(), total, grade);
    }

    /**
     * 部门考核进度列表（运营管理部/委员会/绩效管理员看板用）。
     *
     * <p>数据范围：部门绩效专员/部门负责人仅见本部门；运营/委员会/绩效管理员见全部。
     *
     * @return 部门考核列表（含部门名）
     */
    public List<DeptAssessmentResp> listAll() {
        QueryWrapper<DeptAssessment> qw = new QueryWrapper<>();
        String role = DataScopeContext.current().getPrimaryRole();
        if (RoleConst.ROLE_DEPT_STAFF.equals(role) || RoleConst.ROLE_DEPT_LEAD.equals(role)) {
            Long deptId = DataScopeContext.currentDeptId();
            if (deptId == null) {
                return Collections.emptyList();
            }
            qw.eq("dept_id", deptId);
        }
        qw.orderByDesc("id");
        List<DeptAssessment> list = deptAssessmentMapper.selectList(qw);
        List<DeptAssessmentResp> out = new ArrayList<>(list.size());
        for (DeptAssessment d : list) {
            out.add(toResp(d));
        }
        return out;
    }

    /**
     * 计算单行得分：得分 = min(实际完成值/目标值, 1) × 权重，保留 2 位小数。
     *
     * <p>目标值或实际值为非数字的定性指标、目标值缺失、权重缺失时按 0 分计入。
     *
     * @param targetValue 目标值（Excel 导入，可为非数字）
     * @param actualValue 实际完成值（绩效专员填报）
     * @param weight      权重（0-100，百分比）
     * @return 自动计算得分
     */
    static BigDecimal calcRowScore(String targetValue, String actualValue, BigDecimal weight) {
        if (weight == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal target = toDecimal(targetValue);
        BigDecimal actual = toDecimal(actualValue);
        if (target == null || actual == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal ratio = actual.divide(target, RATIO_SCALE, RoundingMode.HALF_UP);
        if (ratio.compareTo(RATIO_CAP) > 0) {
            ratio = RATIO_CAP;
        }
        return ratio.multiply(weight).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    // ==================== 内部方法 ====================

    /** 初始化部门考核主表 + 默认 KPI 行模板 */
    private DeptAssessment initAssessment(Long periodId, Long deptId) {
        DeptAssessment assessment = new DeptAssessment();
        assessment.setPeriodId(periodId);
        assessment.setDeptId(deptId);
        assessment.setKpiScore(BigDecimal.ZERO);
        assessment.setOperationScore(BigDecimal.ZERO);
        assessment.setKeyWorkScore(BigDecimal.ZERO);
        assessment.setBonusScore(BigDecimal.ZERO);
        assessment.setTotalScore(BigDecimal.ZERO);
        assessment.setStatus(DeptAssessmentState.SELF_FILLING.getCode());
        assessment.setVersion(0);
        deptAssessmentMapper.insert(assessment);
        initDefaultRows(assessment.getId());
        return assessment;
    }

    /** 生成默认 KPI 行模板（全局 seq：经营业绩1-3、运营指标4-5、重点工作6-7） */
    private void initDefaultRows(Long assessmentId) {
        insertRowTemplate(assessmentId, ROW_TYPE_KPI, 1);
        insertRowTemplate(assessmentId, ROW_TYPE_KPI, 2);
        insertRowTemplate(assessmentId, ROW_TYPE_KPI, 3);
        insertRowTemplate(assessmentId, ROW_TYPE_OPERATION, 4);
        insertRowTemplate(assessmentId, ROW_TYPE_OPERATION, 5);
        insertRowTemplate(assessmentId, ROW_TYPE_KEY_WORK, 6);
        insertRowTemplate(assessmentId, ROW_TYPE_KEY_WORK, 7);
    }

    private void insertRowTemplate(Long assessmentId, String rowType, int seqNo) {
        DeptKpiRow row = new DeptKpiRow();
        row.setDeptAssessmentId(assessmentId);
        row.setRowType(rowType);
        row.setSeqNo(seqNo);
        kpiRowMapper.insert(row);
    }

    /** 仅保存实际完成值，其余字段保持模板/HR 导入的内容不变 */
    private void saveActualValues(Long assessmentId, List<DeptActualValueRowReq> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (DeptActualValueRowReq req : rows) {
            if (req.getSeqNo() == null) {
                continue;
            }
            int updated = kpiRowMapper.update(null, new LambdaUpdateWrapper<DeptKpiRow>()
                    .eq(DeptKpiRow::getDeptAssessmentId, assessmentId)
                    .eq(DeptKpiRow::getSeqNo, req.getSeqNo())
                    .set(DeptKpiRow::getActualValue, req.getActualValue()));
            if (updated == 0) {
                throw new BizException(ResultCode.BAD_REQUEST, "KPI 行不存在，请刷新后重试");
            }
        }
    }

    /** 按完成率重算全部行的得分并落库 */
    private void recalcScores(Long assessmentId) {
        List<DeptKpiRow> rows = kpiRowMapper.selectList(
                new QueryWrapper<DeptKpiRow>().eq("dept_assessment_id", assessmentId));
        for (DeptKpiRow row : rows) {
            BigDecimal score = calcRowScore(row.getTargetValue(), row.getActualValue(), row.getWeight());
            kpiRowMapper.update(null, new LambdaUpdateWrapper<DeptKpiRow>()
                    .eq(DeptKpiRow::getId, row.getId())
                    .set(DeptKpiRow::getScore, score));
        }
    }

    /** 退回自评中（复核/初审退回共用） */
    private void rollbackToFilling(DeptAssessment assessment, String comment, String action) {
        int updated = deptAssessmentMapper.update(null, new LambdaUpdateWrapper<DeptAssessment>()
                .eq(DeptAssessment::getId, assessment.getId())
                .eq(DeptAssessment::getStatus, assessment.getStatus())
                .set(DeptAssessment::getStatus, DeptAssessmentState.SELF_FILLING.getCode())
                .set(DeptAssessment::getAdjustReason, action + ": " + comment));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "部门考核状态已变化，请刷新后重试");
        }
        log.info("{}: assessmentId={}, comment={}", action, assessment.getId(), comment);
    }

    /** 计算部门总分 = KPI + 运营 + 重点工作 + 加减分（按行得分求和） */
    private BigDecimal calcTotalScore(Long assessmentId) {
        List<DeptKpiRow> rows = kpiRowMapper.selectList(
                new QueryWrapper<DeptKpiRow>().eq("dept_assessment_id", assessmentId));
        BigDecimal kpi = BigDecimal.ZERO;
        BigDecimal operation = BigDecimal.ZERO;
        BigDecimal keyWork = BigDecimal.ZERO;
        BigDecimal bonus = BigDecimal.ZERO;
        for (DeptKpiRow r : rows) {
            BigDecimal score = r.getScore() == null ? BigDecimal.ZERO : r.getScore();
            // 按模板 seq 段判定类型（1-3 经营业绩、4-5 运营指标、6-7 重点工作），其余计入加减分
            if (r.getSeqNo() != null && r.getSeqNo() <= KPI_ROW_COUNT) {
                kpi = kpi.add(score);
            } else if (r.getSeqNo() != null && r.getSeqNo() <= KPI_ROW_COUNT + OPERATION_ROW_COUNT) {
                operation = operation.add(score);
            } else if (r.getSeqNo() != null
                    && r.getSeqNo() <= KPI_ROW_COUNT + OPERATION_ROW_COUNT + KEY_WORK_ROW_COUNT) {
                keyWork = keyWork.add(score);
            } else {
                bonus = bonus.add(score);
            }
        }
        // 持久化各分项，便于展示与审计
        DeptAssessment update = new DeptAssessment();
        update.setKpiScore(kpi);
        update.setOperationScore(operation);
        update.setKeyWorkScore(keyWork);
        update.setBonusScore(bonus);
        deptAssessmentMapper.update(update, new LambdaUpdateWrapper<DeptAssessment>()
                .eq(DeptAssessment::getId, assessmentId));
        return kpi.add(operation).add(keyWork).add(bonus);
    }

    /** 部门等级：总分 ≥90=A, ≥75=B, ≥60=C, <60=D */
    private String gradeOf(BigDecimal total) {
        double v = total.doubleValue();
        if (v >= 90) return "A";
        if (v >= 75) return "B";
        if (v >= 60) return "C";
        return "D";
    }

    /** 校验周期为进行中，否则抛业务异常 */
    private AssessmentPeriod requireOpenPeriod(Long periodId) {
        AssessmentPeriod period = periodMapper.selectById(periodId);
        if (period == null || !Integer.valueOf(PERIOD_STATUS_OPEN).equals(period.getStatus())) {
            throw new BizException(ResultCode.PERIOD_NOT_OPEN, "考核周期未开启或已结束");
        }
        return period;
    }

    private DeptAssessment requiredById(Long id) {
        DeptAssessment assessment = deptAssessmentMapper.selectById(id);
        if (assessment == null) {
            throw new BizException(ResultCode.DEPT_ASSESS_NOT_FOUND);
        }
        return assessment;
    }

    private DeptAssessment findAssessment(Long periodId, Long deptId) {
        return deptAssessmentMapper.selectOne(new QueryWrapper<DeptAssessment>()
                .eq("period_id", periodId)
                .eq("dept_id", deptId)
                .last("LIMIT 1"));
    }

    /** 校验当前用户为部门绩效专员且属于指定部门 */
    private void assertDeptStaff(Long deptId) {
        String role = DataScopeContext.current().getPrimaryRole();
        if (!RoleConst.ROLE_DEPT_STAFF.equals(role)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        Long currentDeptId = DataScopeContext.currentDeptId();
        if (currentDeptId == null || !currentDeptId.equals(deptId)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能填报本部门考核");
        }
    }

    /** 校验当前用户为部门负责人且属于指定部门 */
    private void assertDeptLead(Long deptId) {
        String role = DataScopeContext.current().getPrimaryRole();
        if (!RoleConst.ROLE_DEPT_LEAD.equals(role)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        Long currentDeptId = DataScopeContext.currentDeptId();
        if (currentDeptId == null || !currentDeptId.equals(deptId)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能复核本部门考核");
        }
    }

    /** 校验当前用户为运营管理部 */
    private void assertOperation() {
        String role = DataScopeContext.current().getPrimaryRole();
        if (!RoleConst.ROLE_OPERATION.equals(role)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    /** 校验当前用户为绩效委员会 */
    private void assertCommittee() {
        String role = DataScopeContext.current().getPrimaryRole();
        if (!RoleConst.ROLE_COMMITTEE.equals(role)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    /** 校验查看权限：专员/负责人仅可见本部门，其余角色可见全部 */
    private void assertViewPermission(Long deptId) {
        String role = DataScopeContext.current().getPrimaryRole();
        if (!RoleConst.ROLE_DEPT_STAFF.equals(role) && !RoleConst.ROLE_DEPT_LEAD.equals(role)) {
            return;
        }
        Long currentDeptId = DataScopeContext.currentDeptId();
        if (currentDeptId == null || !currentDeptId.equals(deptId)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能查看本部门考核");
        }
    }

    private boolean isDeptLinePeriod(AssessmentPeriod period) {
        if (period.getPeriodType() == null) {
            return false;
        }
        try {
            return PeriodType.of(period.getPeriodType()).isDeptLine();
        } catch (IllegalArgumentException e) {
            log.warn("未知周期类型: periodId={}, periodType={}", period.getId(), period.getPeriodType());
            return false;
        }
    }

    private DeptAssessmentOptionResp toOptionResp(AssessmentPeriod p, DeptAssessment a) {
        DeptAssessmentOptionResp resp = new DeptAssessmentOptionResp();
        resp.setAssessmentId(a.getId());
        resp.setPeriodId(p.getId());
        resp.setPeriodName(p.getName());
        resp.setPeriodType(p.getPeriodType());
        resp.setPeriodTypeLabel(PeriodType.of(p.getPeriodType()).getLabel());
        resp.setYear(p.getYear());
        resp.setQuarter(p.getQuarter());
        resp.setDeptId(a.getDeptId());
        SysDepartment dept = deptMapper.selectById(a.getDeptId());
        resp.setDeptName(dept == null ? null : dept.getName());
        resp.setStatus(a.getStatus());
        resp.setSubmittedAt(a.getSubmittedAt());
        return resp;
    }

    private DeptAssessmentResp toResp(DeptAssessment d) {
        DeptAssessmentResp resp = new DeptAssessmentResp();
        resp.setId(d.getId());
        resp.setPeriodId(d.getPeriodId());
        resp.setDeptId(d.getDeptId());
        SysDepartment dept = deptMapper.selectById(d.getDeptId());
        resp.setDeptName(dept == null ? null : dept.getName());
        resp.setKpiScore(d.getKpiScore());
        resp.setOperationScore(d.getOperationScore());
        resp.setKeyWorkScore(d.getKeyWorkScore());
        resp.setBonusScore(d.getBonusScore());
        resp.setTotalScore(d.getTotalScore());
        resp.setDeptGrade(d.getDeptGrade());
        resp.setStatus(d.getStatus());
        resp.setSubmittedAt(d.getSubmittedAt());
        resp.setReviewedAt(d.getReviewedAt());
        resp.setApprovedAt(d.getApprovedAt());
        resp.setVersion(d.getVersion());
        resp.setAdjustReason(d.getAdjustReason());
        resp.setRows(listRowResps(d.getId()));
        return resp;
    }

    private List<DeptKpiRowResp> listRowResps(Long assessmentId) {
        List<DeptKpiRow> rows = kpiRowMapper.selectList(
                new QueryWrapper<DeptKpiRow>()
                        .eq("dept_assessment_id", assessmentId)
                        .orderByAsc("row_type")
                        .orderByAsc("seq_no"));
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<DeptKpiRowResp> out = new ArrayList<>(rows.size());
        for (DeptKpiRow r : rows) {
            DeptKpiRowResp resp = new DeptKpiRowResp();
            resp.setId(r.getId());
            resp.setDeptAssessmentId(r.getDeptAssessmentId());
            resp.setRowType(r.getRowType());
            resp.setSeqNo(r.getSeqNo());
            resp.setIndicatorName(r.getIndicatorName());
            resp.setTargetValue(r.getTargetValue());
            resp.setActualValue(r.getActualValue());
            resp.setScoringStandard(r.getScoringStandard());
            resp.setScore(r.getScore());
            resp.setWeight(r.getWeight());
            out.add(resp);
        }
        return out;
    }

    private static BigDecimal toDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
