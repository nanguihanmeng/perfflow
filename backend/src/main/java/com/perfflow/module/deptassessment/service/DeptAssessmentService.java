package com.perfflow.module.deptassessment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.deptassessment.dto.DeptAssessmentReq;
import com.perfflow.module.deptassessment.dto.DeptAssessmentResp;
import com.perfflow.module.deptassessment.dto.DeptKpiRowReq;
import com.perfflow.module.deptassessment.dto.DeptKpiRowResp;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import com.perfflow.module.deptassessment.entity.DeptKpiRow;
import com.perfflow.module.deptassessment.enums.DeptAssessmentState;
import com.perfflow.module.deptassessment.mapper.DeptAssessmentMapper;
import com.perfflow.module.deptassessment.mapper.DeptKpiRowMapper;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 部门考核服务：KPI 填报、复核、初审、审批、部门等级自动计算。
 *
 * <p>状态流转采用"原子条件更新 + updated==0 抛错"范式，防止并发重复流转。
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
            DeptAssessment exists = deptAssessmentMapper.selectOne(
                    new QueryWrapper<DeptAssessment>()
                            .eq("period_id", period.getId())
                            .eq("dept_id", d.getId())
                            .last("LIMIT 1"));
            if (exists != null) {
                continue;
            }
            initAssessment(period.getId(), d.getId());
        }
        log.info("部门类周期开启初始化完成: periodId={}, 部门数={}, 生成主表数={}",
                period.getId(), depts.size(), depts.size());
    }

    /**
     * 获取当前周期本部门考核表（不存在则初始化）。
     *
     * @param deptId 部门ID
     * @return 部门考核响应
     */
    @Transactional(rollbackFor = Exception.class)
    public DeptAssessmentResp current(Long deptId) {
        AssessmentPeriod period = requiredActivePeriod();
        DeptAssessment assessment = deptAssessmentMapper.selectOne(
                new QueryWrapper<DeptAssessment>()
                        .eq("period_id", period.getId())
                        .eq("dept_id", deptId)
                        .last("LIMIT 1"));
        if (assessment == null) {
            assessment = initAssessment(period.getId(), deptId);
        }
        return toResp(assessment);
    }

    /**
     * 部门绩效专员填报/提交本部门 KPI（自评中 → 待复核）。
     *
     * @param deptId 部门ID
     * @param req    填报请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long deptId, DeptAssessmentReq req) {
        assertDeptStaff(deptId);
        AssessmentPeriod period = requiredActivePeriod();
        DeptAssessment assessment = deptAssessmentMapper.selectOne(
                new QueryWrapper<DeptAssessment>()
                        .eq("period_id", period.getId())
                        .eq("dept_id", deptId)
                        .last("LIMIT 1"));
        if (assessment == null) {
            assessment = initAssessment(period.getId(), deptId);
        }
        stateMachine.assertInState(assessment, DeptAssessmentState.SELF_FILLING);

        // 保存 KPI 行
        saveRows(assessment.getId(), req.getRows());

        // 原子条件更新：仅当仍处于自评中才流转
        int updated = deptAssessmentMapper.update(null, new LambdaUpdateWrapper<DeptAssessment>()
                .eq(DeptAssessment::getId, assessment.getId())
                .eq(DeptAssessment::getStatus, DeptAssessmentState.SELF_FILLING.getCode())
                .set(DeptAssessment::getStatus, DeptAssessmentState.PENDING_REVIEW.getCode())
                .set(DeptAssessment::getSubmittedAt, LocalDateTime.now())
                .set(DeptAssessment::getVersion, (assessment.getVersion() == null ? 0 : assessment.getVersion()) + 1));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "部门考核状态已变化，请刷新后重试");
        }
        log.info("部门绩效专员提交部门考核: deptId={}, periodId={}", deptId, period.getId());
    }

    /**
     * 部门负责人复核（待复核 → 待初审 / 退回自评中）。
     *
     * @param deptId  部门ID
     * @param approve 是否通过
     * @param comment 意见/原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void review(Long deptId, boolean approve, String comment) {
        assertDeptLead(deptId);
        AssessmentPeriod period = requiredActivePeriod();
        DeptAssessment assessment = requiredByPeriodDept(period.getId(), deptId);
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
        log.info("部门负责人复核通过: deptId={}, periodId={}", deptId, period.getId());
    }

    /**
     * 运营管理部初审（待初审 → 待审批 / 退回整改）。
     *
     * @param deptId  部门ID
     * @param approve 是否通过
     * @param comment 意见/原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long deptId, boolean approve, String comment) {
        assertOperation();
        AssessmentPeriod period = requiredActivePeriod();
        DeptAssessment assessment = requiredByPeriodDept(period.getId(), deptId);
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
        log.info("运营管理部初审通过: deptId={}, periodId={}", deptId, period.getId());
    }

    /**
     * 绩效委员会最终审批（待审批 → 已完成，触发部门等级计算）。
     *
     * @param deptId 部门ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long deptId) {
        assertCommittee();
        AssessmentPeriod period = requiredActivePeriod();
        DeptAssessment assessment = requiredByPeriodDept(period.getId(), deptId);
        stateMachine.assertInState(assessment, DeptAssessmentState.PENDING_APPROVE);

        // 计算部门总分与等级
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
        log.info("绩效委员会审批完成: deptId={}, periodId={}, totalScore={}, grade={}",
                deptId, period.getId(), total, grade);
    }

    /**
     * 部门考核进度列表（运营管理部/委员会/管理员看板用）。
     *
     * <p>数据范围：部门绩效专员/部门负责人仅见本部门；运营/委员会/管理员见全部。
     *
     * @return 部门考核列表（含部门名）
     */
    public List<DeptAssessmentResp> listAll() {
        QueryWrapper<DeptAssessment> qw = new QueryWrapper<>();
        String role = DataScopeContext.current().getPrimaryRole();
        if (RoleConst.ROLE_DEPT_STAFF.equals(role) || RoleConst.ROLE_DEPT_LEAD.equals(role)) {
            Long deptId = DataScopeContext.currentDeptId();
            if (deptId != null) {
                qw.eq("dept_id", deptId);
            } else {
                // 无部门归属时返回空
                return Collections.emptyList();
            }
        }
        qw.orderByDesc("id");
        List<DeptAssessment> list = deptAssessmentMapper.selectList(qw);
        List<DeptAssessmentResp> out = new ArrayList<>(list.size());
        for (DeptAssessment d : list) {
            out.add(toResp(d));
        }
        return out;
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

    /** 保存 KPI 行（按 dept_assessment_id + seq_no 覆盖写，幂等；行类型以模板为准，不随请求覆盖） */
    private void saveRows(Long assessmentId, List<DeptKpiRowReq> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (DeptKpiRowReq req : rows) {
            DeptKpiRow row = kpiRowMapper.selectOne(new QueryWrapper<DeptKpiRow>()
                    .eq("dept_assessment_id", assessmentId)
                    .eq("seq_no", req.getSeqNo())
                    .last("LIMIT 1"));
            if (row == null) {
                row = new DeptKpiRow();
                row.setDeptAssessmentId(assessmentId);
                row.setSeqNo(req.getSeqNo());
                row.setRowType(req.getRowType());
            }
            row.setIndicatorName(req.getIndicatorName());
            row.setTargetValue(req.getTargetValue());
            row.setActualValue(req.getActualValue());
            row.setScoringStandard(req.getScoringStandard());
            row.setScore(req.getScore());
            row.setWeight(req.getWeight());
            if (row.getId() == null) {
                kpiRowMapper.insert(row);
            } else {
                kpiRowMapper.updateById(row);
            }
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
        log.info("{}: deptId={}, comment={}", action, assessment.getDeptId(), comment);
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

    /** 当前活跃周期（未结束），不存在抛业务异常 */
    private AssessmentPeriod requiredActivePeriod() {
        AssessmentPeriod period = periodMapper.selectOne(new QueryWrapper<AssessmentPeriod>()
                .orderByDesc("year").orderByDesc("quarter").last("LIMIT 1"));
        if (period == null) {
            throw new BizException(ResultCode.PERIOD_NOT_OPEN, "暂无考核周期");
        }
        return period;
    }

    private DeptAssessment requiredByPeriodDept(Long periodId, Long deptId) {
        DeptAssessment assessment = deptAssessmentMapper.selectOne(
                new QueryWrapper<DeptAssessment>()
                        .eq("period_id", periodId)
                        .eq("dept_id", deptId)
                        .last("LIMIT 1"));
        if (assessment == null) {
            throw new BizException(ResultCode.NOT_FOUND, "部门考核表不存在");
        }
        return assessment;
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
}
