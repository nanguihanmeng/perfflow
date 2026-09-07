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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
/**
 * 部门考核服务：负责部门 KPI 指标填报与实际值录入、部门负责人复核、绩效初审与审批，
 * 并自动完成部门得分汇总与等级计算。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptAssessmentService {


    public static final String ROW_TYPE_KPI = "经营业绩";
    public static final String ROW_TYPE_OPERATION = "运营指标";
    public static final String ROW_TYPE_KEY_WORK = "重点工作";

    private static final int KPI_ROW_COUNT = 3;
    private static final int OPERATION_ROW_COUNT = 2;
    private static final int KEY_WORK_ROW_COUNT = 2;

    private static final int PERIOD_STATUS_OPEN = 1;

    private static final BigDecimal RATIO_CAP = BigDecimal.ONE;

    private static final int RATIO_SCALE = 4;

    private static final int SCORE_SCALE = 2;

    private static final double GRADE_A_THRESHOLD = 90;
    private static final double GRADE_B_THRESHOLD = 75;
    private static final double GRADE_C_THRESHOLD = 60;
    private final DeptAssessmentMapper deptAssessmentMapper;
    private final DeptKpiRowMapper kpiRowMapper;
    private final AssessmentPeriodMapper periodMapper;
    private final SysDepartmentMapper deptMapper;
    private final DeptAssessmentStateMachine stateMachine;
    private final DeptAssessmentFlowService flowService;
    /**
     * 原子条件更新：仅当考核仍处于 fromStatus 时才置为 toStatus，避免并发重复流转；
     * 返回是否更新成功。
     */
    private boolean transitionStatus(Long assessmentId, DeptAssessmentState fromStatus,
                                     DeptAssessmentState toStatus,
                                     Consumer<LambdaUpdateWrapper<DeptAssessment>> extraSetter) {
        LambdaUpdateWrapper<DeptAssessment> wrapper = new LambdaUpdateWrapper<DeptAssessment>()
                .eq(DeptAssessment::getId, assessmentId)
                .eq(DeptAssessment::getStatus, fromStatus.getCode())
                .set(DeptAssessment::getStatus, toStatus.getCode());
        // 非空才处理
        if (extraSetter != null) {
            extraSetter.accept(wrapper);
        }
        // 更新记录
        return deptAssessmentMapper.update(null, wrapper) > 0;
    }

    //
    private void assertTransitioned(boolean ok) {
        // 校验执行结果
        if (!ok) {
            // 校验失败抛异常
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "部门考核状态已变化，请刷新后重试");
        }
    }

     // 部门类周期开启时，为勾选部门（或全部部门）生成部门考核主表 + KPI 行模板。

    @Transactional(rollbackFor = Exception.class)
    public void initForPeriod(AssessmentPeriod period, List<Long> deptIds) {
        QueryWrapper<SysDepartment> qw = new QueryWrapper<>();
        // 非空才处理
        if (deptIds != null && !deptIds.isEmpty()) {
            qw.in("id", deptIds);
        }
        qw.orderByAsc("id");
        // 查询列表
        List<SysDepartment> depts = deptMapper.selectList(qw);
        for (SysDepartment d : depts) {
            // 判空处理
            if (d.getId() == null) {
                continue;
            }
            // 已生成过则跳过，保证幂等
            if (findAssessment(period.getId(), d.getId()) != null) {
                continue;
            }
            initAssessment(period.getId(), d.getId());
        }
        log.info("部门类周期开启初始化完成: periodId={}, 部门数={}, 生成主表数={}",
                period.getId(), depts.size(), depts.size());
    }

     // 查询本部门可填报的部门考核选项（仅 HR 已开启的部门线周期）。

    public List<DeptAssessmentOptionResp> listOptions(Long deptId) {
        // 查询列表
        List<AssessmentPeriod> periods = periodMapper.selectList(new QueryWrapper<AssessmentPeriod>()
                .eq("status", PERIOD_STATUS_OPEN)
                .orderByDesc("year")
                .orderByDesc("quarter"));
        // 构建集合容器
        List<DeptAssessmentOptionResp> out = new ArrayList<>(periods.size());
        for (AssessmentPeriod p : periods) {
            // 仅部门线周期且本部门已生成考核表才作为选项
            if (!isDeptLinePeriod(p)) {
                continue;
            }
            // 查询考核主表
            DeptAssessment assessment = findAssessment(p.getId(), deptId);
            // 判空处理
            if (assessment == null) {
                continue;
            }
            // 转换响应对象
            out.add(toOptionResp(p, assessment));
        }
        // 返回结果
        return out;
    }

     // 按主表ID查询部门考核详情（含 KPI 行）。

    public DeptAssessmentResp getById(Long id) {
        // 加载实体并校验存在
        DeptAssessment assessment = requiredById(id);
        // 权限或数据校验
        assertViewPermission(assessment.getDeptId());
        // 转换响应对象
        return toResp(assessment);
    }

     // 部门绩效专员提交本部门 KPI 实际完成值（自评中 → 待复核）。
     // 得分在提交时由系统按完成率自动重算。

    @Transactional(rollbackFor = Exception.class)
    public void submit(Long assessmentId, DeptActualValueReq req) {
        // 加载考核主表并校验专员归属与周期状态
        DeptAssessment assessment = requiredById(assessmentId);
        // 权限或数据校验
        assertDeptStaff(assessment.getDeptId());
        // 校验周期进行中
        requireOpenPeriod(assessment.getPeriodId());
        // 校验当前状态
        stateMachine.assertInState(assessment, DeptAssessmentState.SELF_FILLING);
        // 仅保存实际完成值，得分自动计算
        saveActualValues(assessment.getId(), req.getRows());
        // 重算得分
        recalcScores(assessment.getId());
        // 原子条件更新：仅当仍处于自评中才流转
        boolean ok = transitionStatus(assessment.getId(), DeptAssessmentState.SELF_FILLING,
                DeptAssessmentState.PENDING_REVIEW,
                w -> {
                    w.set(DeptAssessment::getSubmittedAt, LocalDateTime.now());
                    w.set(DeptAssessment::getVersion,
                            (assessment.getVersion() == null ? 0 : assessment.getVersion()) + 1);
                });
        // 校验流转成功
        assertTransitioned(ok);
        // 记录提交留痕
        flowService.writeLog(assessment, DeptAssessmentState.SELF_FILLING,
                DeptAssessmentState.PENDING_REVIEW, DeptAssessmentFlowService.ACTION_SUBMIT, null);
        log.info("部门绩效专员提交部门考核: assessmentId={}, deptId={}", assessmentId, assessment.getDeptId());
    }

     // 部门负责人复核（待复核 → 待初审 / 退回自评中）。

    @Transactional(rollbackFor = Exception.class)
    public void review(Long assessmentId, boolean approve, String comment) {
        // 加载实体并校验存在
        DeptAssessment assessment = requiredById(assessmentId);
        // 权限或数据校验
        assertDeptLead(assessment.getDeptId());
        // 校验周期进行中
        requireOpenPeriod(assessment.getPeriodId());
        // 校验当前状态
        stateMachine.assertInState(assessment, DeptAssessmentState.PENDING_REVIEW);
        // 不通过则退回自评中并记录原因
        if (!approve) {
            // 退回自评中
            rollbackToFilling(assessment, comment, "复核退回");
            // 记录流程留痕
            flowService.writeLog(assessment, DeptAssessmentState.PENDING_REVIEW,
                    DeptAssessmentState.SELF_FILLING, DeptAssessmentFlowService.ACTION_REVIEW_REJECT, comment);
            return;
        }
        // 复核通过流转到待初审
        boolean ok = transitionStatus(assessment.getId(), DeptAssessmentState.PENDING_REVIEW,
                DeptAssessmentState.PENDING_AUDIT,
                w -> w.set(DeptAssessment::getReviewedAt, LocalDateTime.now()));
        // 校验流转成功
        assertTransitioned(ok);
        // 记录流程留痕
        flowService.writeLog(assessment, DeptAssessmentState.PENDING_REVIEW,
                DeptAssessmentState.PENDING_AUDIT, DeptAssessmentFlowService.ACTION_REVIEW_PASS, comment);
        log.info("部门负责人复核通过: assessmentId={}, deptId={}", assessmentId, assessment.getDeptId());
    }

     // 运营管理部初审（待初审 → 待审批 / 退回整改）。

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long assessmentId, boolean approve, String comment) {
        // 权限或数据校验
        assertOperation();
        // 加载实体并校验存在
        DeptAssessment assessment = requiredById(assessmentId);
        // 校验周期进行中
        requireOpenPeriod(assessment.getPeriodId());
        // 校验当前状态
        stateMachine.assertInState(assessment, DeptAssessmentState.PENDING_AUDIT);
        // 不通过则退回自评中整改并记录原因
        if (!approve) {
            // 退回自评中
            rollbackToFilling(assessment, comment, "初审退回整改");
            // 记录流程留痕
            flowService.writeLog(assessment, DeptAssessmentState.PENDING_AUDIT,
                    DeptAssessmentState.SELF_FILLING, DeptAssessmentFlowService.ACTION_AUDIT_REJECT, comment);
            return;
        }
        // 初审通过流转到待审批
        boolean ok = transitionStatus(assessment.getId(), DeptAssessmentState.PENDING_AUDIT,
                DeptAssessmentState.PENDING_APPROVE, null);
        // 校验流转成功
        assertTransitioned(ok);
        // 记录流程留痕
        flowService.writeLog(assessment, DeptAssessmentState.PENDING_AUDIT,
                DeptAssessmentState.PENDING_APPROVE, DeptAssessmentFlowService.ACTION_AUDIT_PASS, comment);
        log.info("运营管理部初审通过: assessmentId={}, deptId={}", assessmentId, assessment.getDeptId());
    }

     // 绩效委员会最终审批（待审批 → 已完成，触发部门等级计算）。

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long assessmentId) {
        // 权限或数据校验
        assertCommittee();
        // 加载实体并校验存在
        DeptAssessment assessment = requiredById(assessmentId);
        // 校验周期进行中
        requireOpenPeriod(assessment.getPeriodId());
        // 校验当前状态
        stateMachine.assertInState(assessment, DeptAssessmentState.PENDING_APPROVE);
        // 审批前重算得分，确保与最新实际完成值一致
        recalcScores(assessment.getId());
        // 计算等级与总分
        BigDecimal total = calcTotalScore(assessment.getId());
        // 计算等级与总分
        String grade = gradeOf(total);
        // 原子条件更新：仅当仍处于待审批才流转
        boolean ok = transitionStatus(assessment.getId(), DeptAssessmentState.PENDING_APPROVE,
                DeptAssessmentState.COMPLETED,
                w -> {
                    w.set(DeptAssessment::getTotalScore, total);
                    w.set(DeptAssessment::getDeptGrade, grade);
                    w.set(DeptAssessment::getApprovedAt, LocalDateTime.now());
                });
        // 校验流转成功
        assertTransitioned(ok);
        // 记录流程留痕
        flowService.writeLog(assessment, DeptAssessmentState.PENDING_APPROVE,
                DeptAssessmentState.COMPLETED, DeptAssessmentFlowService.ACTION_APPROVE, null);
        log.info("绩效委员会审批完成: assessmentId={}, deptId={}, totalScore={}, grade={}",
                assessmentId, assessment.getDeptId(), total, grade);
    }

     // 部门考核进度列表（运营管理部/委员会/绩效管理员看板用）。

    public List<DeptAssessmentResp> listAll() {
        QueryWrapper<DeptAssessment> qw = new QueryWrapper<>();
        // 取当前用户上下文
        String role = DataScopeContext.current().getPrimaryRole();
        // 专员与负责人限定本部门数据
        if (RoleConst.ROLE_DEPT_STAFF.equals(role) || RoleConst.ROLE_DEPT_LEAD.equals(role)) {
            // 取当前用户上下文
            Long deptId = DataScopeContext.currentDeptId();
            // 判空处理
            if (deptId == null) {
                // 空结果返回空集合
                return Collections.emptyList();
            }
            qw.eq("dept_id", deptId);
        }
        qw.orderByDesc("id");
        // 查询列表
        List<DeptAssessment> list = deptAssessmentMapper.selectList(qw);

        // 批量加载部门名，避免列表 N+1 查询
        Map<Long, String> deptNames = new HashMap<>();
        List<Long> deptIds = list.stream().map(DeptAssessment::getDeptId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!deptIds.isEmpty()) {
            for (SysDepartment dept : deptMapper.selectBatchIds(deptIds)) {
                deptNames.put(dept.getId(), dept.getName());
            }
        }

        // 批量加载 KPI 行（一次 IN 查询，分组后顺序与单表查询一致）
        Map<Long, List<DeptKpiRowResp>> rowsByAssessment = new HashMap<>();
        if (!list.isEmpty()) {
            List<Long> ids = list.stream().map(DeptAssessment::getId).collect(Collectors.toList());
            List<DeptKpiRow> rows = kpiRowMapper.selectList(new QueryWrapper<DeptKpiRow>()
                    .in("dept_assessment_id", ids)
                    .orderByAsc("row_type")
                    .orderByAsc("seq_no"));
            for (DeptKpiRow r : rows) {
                rowsByAssessment.computeIfAbsent(r.getDeptAssessmentId(), k -> new ArrayList<>()).add(toRowResp(r));
            }
        }

        // 构建集合容器
        List<DeptAssessmentResp> out = new ArrayList<>(list.size());
        for (DeptAssessment d : list) {
            out.add(toResp(d, deptNames.get(d.getDeptId()),
                    rowsByAssessment.getOrDefault(d.getId(), Collections.emptyList())));
        }
        // 返回结果
        return out;
    }

     // 计算单行得分：得分 = min(实际完成值/目标值, 1) × 权重，保留 2 位小数。

    static BigDecimal calcRowScore(String targetValue, String actualValue, BigDecimal weight) {
        // 权重缺失直接记 0 分
        if (weight == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal target = toDecimal(targetValue);
        BigDecimal actual = toDecimal(actualValue);
        // 目标值或实际值无法解析、目标值非正数时记 0 分
        if (target == null || actual == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal ratio = actual.divide(target, RATIO_SCALE, RoundingMode.HALF_UP);
        // 超额完成按 100% 封顶
        if (ratio.compareTo(RATIO_CAP) > 0) {
            ratio = RATIO_CAP;
        }
        return ratio.multiply(weight).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    // ==================== 内部方法 ====================

    private DeptAssessment initAssessment(Long periodId, Long deptId) {
        DeptAssessment assessment = new DeptAssessment();
        assessment.setPeriodId(periodId);
        assessment.setDeptId(deptId);
        assessment.setKpiScore(BigDecimal.ZERO);
        assessment.setOperationScore(BigDecimal.ZERO);
        assessment.setKeyWorkScore(BigDecimal.ZERO);
        assessment.setBonusScore(BigDecimal.ZERO);
        assessment.setTotalScore(BigDecimal.ZERO);
        // 设置状态
        assessment.setStatus(DeptAssessmentState.SELF_FILLING.getCode());
        assessment.setVersion(0);
        // 写入记录
        deptAssessmentMapper.insert(assessment);
        initDefaultRows(assessment.getId());
        return assessment;
    }

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
        // 写入记录
        kpiRowMapper.insert(row);
    }

    private void saveActualValues(Long assessmentId, List<DeptActualValueRowReq> rows) {
        // 判空处理
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (DeptActualValueRowReq req : rows) {
            // 判空处理
            if (req.getSeqNo() == null) {
                continue;
            }
            // 更新记录
            int updated = kpiRowMapper.update(null, new LambdaUpdateWrapper<DeptKpiRow>()
                    .eq(DeptKpiRow::getDeptAssessmentId, assessmentId)
                    .eq(DeptKpiRow::getSeqNo, req.getSeqNo())
                    .set(DeptKpiRow::getActualValue, req.getActualValue()));
            // 数值判断
            if (updated == 0) {
                // 校验失败抛异常
                throw new BizException(ResultCode.BAD_REQUEST, "KPI 行不存在，请刷新后重试");
            }
        }
    }

    private void recalcScores(Long assessmentId) {
        // 查询列表
        List<DeptKpiRow> rows = kpiRowMapper.selectList(
                new QueryWrapper<DeptKpiRow>().eq("dept_assessment_id", assessmentId));
        for (DeptKpiRow row : rows) {
            // 计算得分
            BigDecimal score = calcRowScore(row.getTargetValue(), row.getActualValue(), row.getWeight());
            // 更新记录
            kpiRowMapper.update(null, new LambdaUpdateWrapper<DeptKpiRow>()
                    .eq(DeptKpiRow::getId, row.getId())
                    .set(DeptKpiRow::getScore, score));
        }
    }

    private void rollbackToFilling(DeptAssessment assessment, String comment, String action) {
        // 原子状态流转
        boolean ok = transitionStatus(assessment.getId(),
                DeptAssessmentState.of(assessment.getStatus()), DeptAssessmentState.SELF_FILLING,
                w -> w.set(DeptAssessment::getAdjustReason, action + ": " + comment));
        // 校验流转成功
        assertTransitioned(ok);
        log.info("{}: assessmentId={}, comment={}", action, assessment.getId(), comment);
    }

    private BigDecimal calcTotalScore(Long assessmentId) {
        // 查询列表
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
        // 更新记录
        deptAssessmentMapper.update(update, new LambdaUpdateWrapper<DeptAssessment>()
                .eq(DeptAssessment::getId, assessmentId));
        return kpi.add(operation).add(keyWork).add(bonus);
    }

    private String gradeOf(BigDecimal total) {
        double v = total.doubleValue();
        if (v >= GRADE_A_THRESHOLD) return "A";
        if (v >= GRADE_B_THRESHOLD) return "B";
        if (v >= GRADE_C_THRESHOLD) return "C";
        return "D";
    }

    private AssessmentPeriod requireOpenPeriod(Long periodId) {
        // 查询单条
        AssessmentPeriod period = periodMapper.selectById(periodId);
        // 判空处理
        if (period == null || !Integer.valueOf(PERIOD_STATUS_OPEN).equals(period.getStatus())) {
            // 校验失败抛异常
            throw new BizException(ResultCode.PERIOD_NOT_OPEN, "考核周期未开启或已结束");
        }
        return period;
    }

    private DeptAssessment requiredById(Long id) {
        // 查询单条
        DeptAssessment assessment = deptAssessmentMapper.selectById(id);
        // 判空处理
        if (assessment == null) {
            // 校验失败抛异常
            throw new BizException(ResultCode.DEPT_ASSESS_NOT_FOUND);
        }
        return assessment;
    }

    private DeptAssessment findAssessment(Long periodId, Long deptId) {
        // 查询单条
        return deptAssessmentMapper.selectOne(new QueryWrapper<DeptAssessment>()
                .eq("period_id", periodId)
                .eq("dept_id", deptId)
                .last("LIMIT 1"));
    }

    private void assertDeptStaff(Long deptId) {
        // 取当前用户上下文
        String role = DataScopeContext.current().getPrimaryRole();
        // 角色判断
        if (!RoleConst.ROLE_DEPT_STAFF.equals(role)) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 取当前用户上下文
        Long currentDeptId = DataScopeContext.currentDeptId();
        // 判空处理
        if (currentDeptId == null || !currentDeptId.equals(deptId)) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN, "只能填报本部门考核");
        }
    }

    private void assertDeptLead(Long deptId) {
        // 取当前用户上下文
        String role = DataScopeContext.current().getPrimaryRole();
        // 角色判断
        if (!RoleConst.ROLE_DEPT_LEAD.equals(role)) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 取当前用户上下文
        Long currentDeptId = DataScopeContext.currentDeptId();
        // 判空处理
        if (currentDeptId == null || !currentDeptId.equals(deptId)) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN, "只能复核本部门考核");
        }
    }

    private void assertOperation() {
        // 取当前用户上下文
        String role = DataScopeContext.current().getPrimaryRole();
        // 角色判断
        if (!RoleConst.ROLE_OPERATION.equals(role)) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    private void assertCommittee() {
        // 取当前用户上下文
        String role = DataScopeContext.current().getPrimaryRole();
        // 角色判断
        if (!RoleConst.ROLE_COMMITTEE.equals(role)) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    private void assertViewPermission(Long deptId) {
        // 取当前用户上下文
        String role = DataScopeContext.current().getPrimaryRole();
        // 角色判断
        if (!RoleConst.ROLE_DEPT_STAFF.equals(role) && !RoleConst.ROLE_DEPT_LEAD.equals(role)) {
            return;
        }
        // 取当前用户上下文
        Long currentDeptId = DataScopeContext.currentDeptId();
        // 判空处理
        if (currentDeptId == null || !currentDeptId.equals(deptId)) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN, "只能查看本部门考核");
        }
    }

    private boolean isDeptLinePeriod(AssessmentPeriod period) {
        // 判空处理
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
        // 查询单条
        SysDepartment dept = deptMapper.selectById(a.getDeptId());
        resp.setDeptName(dept == null ? null : dept.getName());
        // 设置状态
        resp.setStatus(a.getStatus());
        resp.setSubmittedAt(a.getSubmittedAt());
        // 返回结果
        return resp;
    }

    private DeptAssessmentResp toResp(DeptAssessment d) {
        // 查询单条
        SysDepartment dept = deptMapper.selectById(d.getDeptId());
        // 返回结果
        return toResp(d, dept == null ? null : dept.getName(), listRowResps(d.getId()));
    }

    // 组装响应（部门名与 KPI 行由调用方提供，供列表批量复用避免 N+1）。
    private DeptAssessmentResp toResp(DeptAssessment d, String deptName, List<DeptKpiRowResp> rows) {
        DeptAssessmentResp resp = new DeptAssessmentResp();
        resp.setId(d.getId());
        resp.setPeriodId(d.getPeriodId());
        resp.setDeptId(d.getDeptId());
        resp.setDeptName(deptName);
        resp.setKpiScore(d.getKpiScore());
        resp.setOperationScore(d.getOperationScore());
        resp.setKeyWorkScore(d.getKeyWorkScore());
        resp.setBonusScore(d.getBonusScore());
        resp.setTotalScore(d.getTotalScore());
        resp.setDeptGrade(d.getDeptGrade());
        // 设置状态
        resp.setStatus(d.getStatus());
        resp.setSubmittedAt(d.getSubmittedAt());
        resp.setReviewedAt(d.getReviewedAt());
        resp.setApprovedAt(d.getApprovedAt());
        resp.setVersion(d.getVersion());
        resp.setAdjustReason(d.getAdjustReason());
        resp.setRows(rows);
        // 返回结果
        return resp;
    }

    private List<DeptKpiRowResp> listRowResps(Long assessmentId) {
        // 查询列表
        List<DeptKpiRow> rows = kpiRowMapper.selectList(
                new QueryWrapper<DeptKpiRow>()
                        .eq("dept_assessment_id", assessmentId)
                        .orderByAsc("row_type")
                        .orderByAsc("seq_no"));
        // 条件分支
        if (rows.isEmpty()) {
            // 空结果返回空集合
            return Collections.emptyList();
        }
        // 构建集合容器
        List<DeptKpiRowResp> out = new ArrayList<>(rows.size());
        for (DeptKpiRow r : rows) {
            out.add(toRowResp(r));
        }
        // 返回结果
        return out;
    }

    // 单行 KPI 转响应对象。
    private DeptKpiRowResp toRowResp(DeptKpiRow r) {
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
        // 返回结果
        return resp;
    }

    private static BigDecimal toDecimal(String value) {
        // 判空处理
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
