package com.perfflow.module.assessment.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.dto.AssessmentTableResp;
import com.perfflow.module.assessment.dto.FlowLogResp;
import com.perfflow.module.assessment.dto.RowResp;
import com.perfflow.module.assessment.entity.AssessmentFlowLog;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.enums.RowCategory;
import com.perfflow.module.assessment.mapper.AssessmentFlowLogMapper;
import com.perfflow.module.assessment.mapper.AssessmentRowMapper;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/**
 * 个人考核主表服务：提供主表/明细查询，以及由状态机驱动的提交、推送、审核、评分、挂起延长等写动作，
 * 并按周期为被考核人生成考核模板。写动作统一走"原子条件更新"，防止并发下重复流转。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentTableService {

    private static final int MAX_SUSPEND_EXTEND_DAYS = 30;
    //
    private static final int USER_STATUS_ENABLED = 1;
    private final AssessmentTableMapper tableMapper;
    private final AssessmentRowMapper rowMapper;
    private final AssessmentFlowLogMapper flowLogMapper;
    private final AssessmentPeriodMapper periodMapper;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final AssessmentStateMachine stateMachine;
    private final AssessmentCalcService calcService;
    private final AssessmentPermissionService perm;
    private final AssessmentFlowService flowService;
    /**
     * 原子条件更新：仅当主表仍处于 fromState 时才置为 toState，避免并发重复流转；
     * 返回是否更新成功。
     */
    private boolean transitionState(Long tableId, AssessmentState fromState,
                                    AssessmentState toState,
                                    Consumer<LambdaUpdateWrapper<AssessmentTable>> extraSetter) {
        LambdaUpdateWrapper<AssessmentTable> wrapper = new LambdaUpdateWrapper<AssessmentTable>()
                .eq(AssessmentTable::getId, tableId)
                .eq(AssessmentTable::getState, fromState.name())
                .set(AssessmentTable::getState, toState.name());
        // 非空才处理
        if (extraSetter != null) {
            extraSetter.accept(wrapper);
        }
        // 更新记录
        return tableMapper.update(null, wrapper) > 0;
    }

    //
    private void assertTransitioned(boolean ok) {
        // 校验执行结果
        if (!ok) {
            // 校验失败抛异常
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "考核表状态已变化，请刷新后重试");
        }
    }

    // ==================== 查询 ====================
    //
     // 按ID查询主表并校验可见性。
     //

     //
    public AssessmentTable getRequired(Long id) {
        // 查询单条
        AssessmentTable t = tableMapper.selectById(id);
        // 判空处理
        if (t == null) {
            // 校验失败抛异常
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // 数据权限处理
        perm.ensureVisible(t);
        return t;
    }

    //

     //

     //
    @Transactional
    public void updateTablePosition(AssessmentTable t) {
        // 更新记录
        tableMapper.updateById(t);
    }

    //

     //

     //
    @Transactional
    public void delete(Long id) {
        // 查询单条
        AssessmentTable t = tableMapper.selectById(id);
        // 判空处理
        if (t == null) {
            // 校验失败抛异常
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // 删除记录
        flowLogMapper.delete(new QueryWrapper<AssessmentFlowLog>().eq("table_id", id));
        // 考核数据访问
        rowMapper.delete(new QueryWrapper<AssessmentRow>().eq("table_id", id));
        // 考核数据访问
        tableMapper.deleteById(id);
    }

    public AssessmentTableResp getDetail(Long id) {
        AssessmentTable t = getRequired(id);
        // 查询列表
        List<AssessmentRow> rows = rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()).orderByAsc("seq"));
        // 数据权限处理
        boolean masked = perm.isRowMasked(t);
        // 组装行明细
        List<RowResp> rowResps = new ArrayList<>();
        for (AssessmentRow r : rows) {
            // 转换响应对象
            rowResps.add(toRowResp(r, masked));
        }
        // 组装流程日志
        List<FlowLogResp> logs = flowService.listLogs(t.getId());
        // 转换响应对象
        AssessmentTableResp resp = toTableResp(t, masked);
        resp.setRows(rowResps);
        resp.setLogs(logs);
        resp.setRealName(lookupUserName(t.getUserId()));
        resp.setDeptName(lookupDeptName(t.getDeptId()));
        resp.setDeptLeadName(lookupDeptLeadName(t.getDeptId()));
        // 返回结果
        return resp;
    }
     // 分页查询考核主表。

    public Page<AssessmentTableResp> listPage(Long periodId, Long deptId, String state,
                                              int pageNo, int pageSize) {
        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        if (periodId != null) qw.eq("period_id", periodId);
        if (deptId != null) qw.eq("dept_id", deptId);
        if (StringUtils.hasText(state)) qw.eq("state", state);
        // 按角色限定数据范围
        qw = perm.scopeOf(qw);
        qw.orderByDesc("id");
        // 考核数据访问
        Page<AssessmentTable> page = tableMapper.selectPage(Page.of(pageNo, pageSize), qw);
        // 构建集合容器
        Page<AssessmentTableResp> out = Page.of(pageNo, pageSize);
        out.setRecords(new ArrayList<>());
        out.setTotal(page.getTotal());
        for (AssessmentTable t : page.getRecords()) {
            // 数据权限处理
            boolean masked = perm.isRowMasked(t);
            // 转换响应对象
            AssessmentTableResp r = toTableResp(t, masked);
            r.setRealName(lookupUserName(t.getUserId()));
            r.setDeptName(lookupDeptName(t.getDeptId()));
            r.setDeptLeadName(lookupDeptLeadName(t.getDeptId()));
            out.getRecords().add(r);
        }
        // 返回结果
        return out;
    }

    // ==================== 状态机写动作 ====================
     // 员工提交自评，自评中流转至自评挂起。
    @Transactional
    public void submit(Long tableId) {
        AssessmentTable t = getRequired(tableId);
        // 校验被考核人本人提交（排除 HR/ADMIN）
        if (!DataScopeContext.currentUserId().equals(t.getUserId())
                // 数据权限处理
                || !RoleConst.ASSESSED_ROLES.contains(perm.currentRole())) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 校验当前状态
        stateMachine.assertInState(t, AssessmentState.SELF_DRAFTING);
        // 校验必填：所有 PLAN/OPEN 行需 HR 已导入指标名称且员工已填完成率（0-100）
        List<AssessmentRow> rows = rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()).orderByAsc("seq"));
        for (AssessmentRow r : rows) {
            if (RowCategory.BONUS.name().equals(r.getCategory())) continue;
            // 条件分支
            if (!StringUtils.hasText(r.getIndicatorName())) {
                // 校验失败抛异常
                throw new BizException(ResultCode.SUBMIT_REQUIRED_FIELDS,
                        "第 " + r.getSeq() + " 行指标尚未由人事导入");
            }
            // 判空处理
            if (r.getCompletionRate() == null) {
                // 校验失败抛异常
                throw new BizException(ResultCode.SUBMIT_REQUIRED_FIELDS,
                        "第 " + r.getSeq() + " 行未填写完成率");
            }
            if (r.getCompletionRate().compareTo(BigDecimal.ZERO) < 0
                    || r.getCompletionRate().compareTo(BigDecimal.valueOf(100)) > 0) {
                // 校验失败抛异常
                throw new BizException(ResultCode.BAD_REQUEST,
                        "第 " + r.getSeq() + " 行完成率需在 0-100 之间");
            }
        }
        // 先重算分数，再流转状态，避免状态已变但分数未落地的中间态
        calcService.recalcByCompletionRate(t);
        // 原子条件更新：仅当仍处于 SELF_DRAFTING 才流转
        boolean ok = transitionState(tableId, AssessmentState.SELF_DRAFTING,
                AssessmentState.SELF_SUSPENDED,
                w -> w.set(AssessmentTable::getSubmittedAt, LocalDateTime.now()));
        // 校验流转成功
        assertTransitioned(ok);
        t.setState(AssessmentState.SELF_SUSPENDED.name());
        t.setSubmittedAt(LocalDateTime.now());
        // 冻结所有行，提交后不可修改
        rowMapper.update(null, new LambdaUpdateWrapper<AssessmentRow>()
                .eq(AssessmentRow::getTableId, t.getId())
                .set(AssessmentRow::getFrozen, true));
        // 记录流程留痕
        flowService.writeLog(t, AssessmentState.SELF_DRAFTING, AssessmentState.SELF_SUSPENDED, "SUBMIT");
        log.info("员工提交考核表: tableId={}, userId={}", tableId, t.getUserId());
    }

     // HR 推送挂起考核表进入部门审核（无部门则直达领导评分）。

    @Transactional
    public void push(Long tableId) {
        AssessmentTable t = getRequired(tableId);
        // 数据权限处理
        if (!perm.isHr()) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 校验当前状态
        stateMachine.assertInState(t, AssessmentState.SELF_SUSPENDED);
        // 无部门（如公司领导）跳过部门审核，直接进入领导评分阶段
        AssessmentState target = t.getDeptId() == null ? AssessmentState.LEAD_SCORING : AssessmentState.DEPT_REVIEW;
        String action = target == AssessmentState.LEAD_SCORING ? "PUSH_DIRECT" : "PUSH";
        // 原子条件更新：仅当仍处于 SELF_SUSPENDED 才流转
        boolean ok = transitionState(tableId, AssessmentState.SELF_SUSPENDED, target,
                w -> w.set(AssessmentTable::getPushedAt, LocalDateTime.now()));
        // 校验流转成功
        assertTransitioned(ok);
        t.setState(target.name());
        t.setPushedAt(LocalDateTime.now());
        // 记录流程留痕
        flowService.writeLog(t, AssessmentState.SELF_SUSPENDED, target, action);
        log.info("人事推送考核表: tableId={}, 目标状态={}", tableId, target);
    }

     // 部门负责人审核通过，流转至领导评分。

    @Transactional
    public void approve(Long tableId, String comment) {
        AssessmentTable t = getRequired(tableId);
        // 取当前用户上下文
        if (!perm.isDeptLead() || !DataScopeContext.currentDeptId().equals(t.getDeptId())) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 校验当前状态
        stateMachine.assertInState(t, AssessmentState.DEPT_REVIEW);
        // 原子条件更新：仅当仍处于 DEPT_REVIEW 才流转
        boolean ok = transitionState(tableId, AssessmentState.DEPT_REVIEW,
                AssessmentState.LEAD_SCORING,
                w -> w.set(AssessmentTable::getDeptApprovedAt, LocalDateTime.now()));
        // 校验流转成功
        assertTransitioned(ok);
        t.setState(AssessmentState.LEAD_SCORING.name());
        t.setDeptApprovedAt(LocalDateTime.now());
        // 记录流程留痕
        flowService.writeLog(t, AssessmentState.DEPT_REVIEW, AssessmentState.LEAD_SCORING, "APPROVE", comment);
        log.info("部门负责人提交考核表给领导: tableId={}, deptId={}", tableId, t.getDeptId());
    }

     // 部门负责人打回自评，解冻行。

    @Transactional
    public void reject(Long tableId, String comment) {
        // 条件分支
        if (!StringUtils.hasText(comment)) {
            // 校验失败抛异常
            throw new BizException(ResultCode.REJECT_COMMENT_REQUIRED);
        }
        AssessmentTable t = getRequired(tableId);
        // 取当前用户上下文
        if (!perm.isDeptLead() || !DataScopeContext.currentDeptId().equals(t.getDeptId())) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 校验当前状态
        stateMachine.assertInState(t, AssessmentState.DEPT_REVIEW);
        // 原子条件更新：仅当仍处于 DEPT_REVIEW 才流转
        boolean ok = transitionState(tableId, AssessmentState.DEPT_REVIEW,
                AssessmentState.SELF_DRAFTING, null);
        // 校验流转成功
        assertTransitioned(ok);
        t.setState(AssessmentState.SELF_DRAFTING.name());
        // 解冻行，允许员工重新填报
        rowMapper.update(null, new LambdaUpdateWrapper<AssessmentRow>()
                .eq(AssessmentRow::getTableId, t.getId())
                .set(AssessmentRow::getFrozen, false));
        // 记录流程留痕
        flowService.writeLog(t, AssessmentState.DEPT_REVIEW, AssessmentState.SELF_DRAFTING, "REJECT", comment);
        log.info("部门负责人打回考核表: tableId={}, comment={}", tableId, comment);
    }

     // 领导/委员会评分，流转至已完成。

    @Transactional
    public void leadScore(Long tableId, BigDecimal leaderScore, String comment) {
        // 判空处理
        if (leaderScore == null) {
            // 校验失败抛异常
            throw new BizException(ResultCode.LEADER_SCORE_REQUIRED);
        }
        // 范围判断
        if (leaderScore.compareTo(BigDecimal.ZERO) < 0 || leaderScore.compareTo(BigDecimal.valueOf(100)) > 0) {
            // 校验失败抛异常
            throw new BizException(ResultCode.SCORE_OUT_OF_RANGE);
        }
        AssessmentTable t = getRequired(tableId);
        // LEAD 不能给自己评分
        boolean selfIsLead = t.getUserId().equals(DataScopeContext.currentUserId())
                // 数据权限处理
                && RoleConst.ROLE_LEAD.equals(perm.currentRole());
        // 条件分支
        if (selfIsLead) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN, "领导不能给自己评分，由绩效委员会评分");
        }
        // 数据权限处理
        if (!perm.isLead() && !RoleConst.ROLE_COMMITTEE.equals(perm.currentRole())) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 委员会只能评 LEAD（公司领导）的表
        if (RoleConst.ROLE_COMMITTEE.equals(perm.currentRole())) {
            // 查询单条
            SysUser target = userMapper.selectById(t.getUserId());
            // 判空处理
            if (target == null || !RoleConst.ROLE_LEAD.equals(target.getRole())) {
                // 校验失败抛异常
                throw new BizException(ResultCode.FORBIDDEN, "绩效委员会仅可评分公司领导的考核表");
            }
        }
        // 校验当前状态
        stateMachine.assertInState(t, AssessmentState.LEAD_SCORING);
        // 原子条件更新：仅当仍处于 LEAD_SCORING 才流转
        boolean ok = transitionState(tableId, AssessmentState.LEAD_SCORING,
                AssessmentState.FINISHED,
                w -> w.set(AssessmentTable::getLeadFinishedAt, LocalDateTime.now()));
        // 校验流转成功
        assertTransitioned(ok);
        t.setState(AssessmentState.FINISHED.name());
        t.setLeadFinishedAt(LocalDateTime.now());
        // 计算最终分与等级
        calcService.finalizeWithLeaderScore(t, leaderScore);
        // 记录流程留痕
        flowService.writeLog(t, AssessmentState.LEAD_SCORING, AssessmentState.FINISHED, "LEAD_SCORE", comment);
        log.info("领导评分完成: tableId={}, score={}", tableId, leaderScore);
    }

     // 延长挂起天数，累计不超过上限。

    @Transactional
    public void extendSuspend(Long tableId, int days, String reason) {
        AssessmentTable t = getRequired(tableId);
        // 数据权限处理
        String role = perm.currentRole();
        // 角色判断
        if (!RoleConst.ROLE_PERFORMANCE_HR.equals(role) && !RoleConst.ROLE_ADMIN.equals(role)) {
            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 范围判断
        if (days <= 0) {
            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "延长天数必须为正数");
        }
        String safeReason = StringUtils.hasText(reason) ? reason.trim() : "未填写原因";
        int next = (t.getSuspendExtendedDays() == null ? 0 : t.getSuspendExtendedDays()) + days;
        // 条件分支
        if (next > MAX_SUSPEND_EXTEND_DAYS) {
            // 校验失败抛异常
            throw new BizException(ResultCode.EXTEND_OVER_LIMIT);
        }
        t.setSuspendExtendedDays(next);
        // 更新记录
        tableMapper.updateById(t);
        // 记录流程留痕
        flowService.writeLog(t, AssessmentState.valueOf(t.getState()), AssessmentState.valueOf(t.getState()),
                "EXTEND_SUSPEND", "延长 " + days + " 天: " + safeReason);
        log.info("延长挂起: tableId={}, days={}, 累计={}", tableId, days, next);
    }

    // ==================== 模板初始化 ====================

     // 按周期与员工查询考核主表。
    public AssessmentTable findByPeriodAndUser(Long periodId, Long userId) {
        // 查询单条
        return tableMapper.selectOne(new QueryWrapper<AssessmentTable>()
                .eq("period_id", periodId).eq("user_id", userId).last("LIMIT 1"));
    }

     // 查询某考核表全部行（按 seq 升序）。

    public List<AssessmentRow> listRows(Long tableId) {
        // 查询列表
        return rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", tableId).orderByAsc("seq"));
    }


    @Transactional
    public void updateRow(AssessmentRow row) {
        // 更新记录
        rowMapper.updateById(row);
    }

     // 为周期内被考核人生成考核主表 + 10 行模板。

    @Transactional
    public void initForPeriod(AssessmentPeriod period, List<Long> userIds) {
        QueryWrapper<SysUser> qw = new QueryWrapper<SysUser>()
                .in("role", RoleConst.ASSESSED_ROLES)
                .eq("status", USER_STATUS_ENABLED);
        // 非空才处理
        if (userIds != null && !userIds.isEmpty()) {
            qw.in("id", userIds);
        }
        // 查询列表
        List<SysUser> users = userMapper.selectList(qw);
        for (SysUser u : users) {
            // 已存在则跳过，保证幂等
            AssessmentTable exists = tableMapper.selectOne(new QueryWrapper<AssessmentTable>()
                    .eq("period_id", period.getId()).eq("user_id", u.getId()));
            // 非空才处理
            if (exists != null) {
                continue;
            }
            AssessmentTable t = new AssessmentTable();
            t.setPeriodId(period.getId());
            t.setUserId(u.getId());
            t.setDeptId(u.getDeptId());
            t.setState(AssessmentState.SELF_DRAFTING.name());
            t.setSelfTotalScore(BigDecimal.ZERO);
            t.setSuspendExtendedDays(0);
            // 写入记录
            tableMapper.insert(t);
            // 创建 10 行模板（指标分数默认 0，由 HR 导入时填写）
            for (int seq = 1; seq <= 10; seq++) {
                RowCategory cat = seq <= 5 ? RowCategory.PLAN : (seq <= 7 ? RowCategory.OPEN : RowCategory.BONUS);
                createRowTemplate(t.getId(), cat, seq, 0);
            }
        }
    }

    private void createRowTemplate(Long tableId, RowCategory cat, int seq, int baseScore) {
        AssessmentRow r = new AssessmentRow();
        r.setTableId(tableId);
        r.setCategory(cat.name());
        r.setSeq(seq);
        r.setBaseScore(BigDecimal.valueOf(baseScore));
        // BONUS 行默认冻结，员工不填写
        r.setFrozen(cat == RowCategory.BONUS);
        // 写入记录
        rowMapper.insert(r);
    }

    // ==================== 辅助 ====================

     // 转换为主表响应对象，支持按数据权限脱敏。

    public AssessmentTableResp toTableResp(AssessmentTable t, boolean masked) {
        AssessmentTableResp r = new AssessmentTableResp();
        r.setId(t.getId());
        r.setPeriodId(t.getPeriodId());
        r.setUserId(t.getUserId());
        r.setDeptId(t.getDeptId());
        r.setPosition(t.getPosition());
        r.setState(t.getState());
        // 条件分支
        if (masked) {
            r.setSelfTotalScore(null);
            r.setLeaderScore(null);
            r.setFinalScore(null);
            r.setGrade(null);
        } else {
            r.setSelfTotalScore(t.getSelfTotalScore());
            r.setLeaderScore(t.getLeaderScore());
            r.setFinalScore(t.getFinalScore());
            r.setGrade(t.getGrade());
        }
        r.setSuspendExtendedDays(t.getSuspendExtendedDays());
        r.setSubmittedAt(t.getSubmittedAt());
        r.setPushedAt(t.getPushedAt());
        r.setDeptApprovedAt(t.getDeptApprovedAt());
        r.setLeadFinishedAt(t.getLeadFinishedAt());
        // 非空才处理
        if (t.getPeriodId() != null) {
            // 查询单条
            AssessmentPeriod p = periodMapper.selectById(t.getPeriodId());
            if (p != null) r.setPeriodName(p.getName());
        }
        return r;
    }

     // 转换为行响应对象，支持脱敏。

    public RowResp toRowResp(AssessmentRow row, boolean masked) {
        RowResp r = new RowResp();
        r.setId(row.getId());
        r.setTableId(row.getTableId());
        r.setCategory(row.getCategory());
        r.setSeq(row.getSeq());
        r.setIndicatorName(row.getIndicatorName());
        r.setBaseScore(row.getBaseScore());
        r.setWorkTarget(row.getWorkTarget());
        r.setScoreCriteria(row.getScoreCriteria());
        r.setCompletionRate(row.getCompletionRate());
        r.setFrozen(row.getFrozen());
        r.setMasked(masked);
        // 条件分支
        if (masked) {
            r.setSelfScore(null);
            r.setLeaderScore(null);
        } else {
            r.setSelfScore(row.getSelfScore());
            r.setLeaderScore(row.getLeaderScore());
        }
        return r;
    }

    private String lookupUserName(Long uid) {
        // 查询单条
        SysUser u = userMapper.selectById(uid);
        return u == null ? null : u.getRealName();
    }

    private String lookupDeptName(Long did) {
        // 查询单条
        SysDepartment d = deptMapper.selectById(did);
        return d == null ? null : d.getName();
    }

    private String lookupDeptLeadName(Long deptId) {
        if (deptId == null) return null;
        // 查询单条
        SysUser lead = userMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("dept_id", deptId).eq("role", RoleConst.ROLE_DEPT_LEAD)
                .eq("status", USER_STATUS_ENABLED).last("LIMIT 1"));
        return lead == null ? null : lead.getRealName();
    }
}
