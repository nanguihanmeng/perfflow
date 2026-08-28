package com.perfflow.module.assessment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.dto.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 考核主表服务：承担状态机驱动的所有写动作 + 查询 + 初始化模板。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentTableService {

    /** 参与个人考核的被考核人角色（排除 ADMIN / PERFORMANCE_HR） */
    private static final List<String> ASSESSED_ROLES = List.of(
            RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_LEAD, RoleConst.ROLE_LEAD,
            RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION, RoleConst.ROLE_COMMITTEE);

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

    // ==================== 查询 ====================

    public AssessmentTable getRequired(Long id) {
        AssessmentTable t = tableMapper.selectById(id);
        if (t == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        perm.ensureVisible(t);
        return t;
    }

    /**
     * 更新主表岗位信息（员工信息栏填写）。
     *
     * @param t 主表（含 position）
     */
    @Transactional
    public void updateTablePosition(AssessmentTable t) {
        tableMapper.updateById(t);
    }

    /**
     * 删除考核主表（HR）。
     *
     * <p>级联删除流程日志与行；表无外键约束，需手动清理。
     *
     * @param id 主表ID
     * @throws BizException 主表不存在时抛出
     */
    @Transactional
    public void delete(Long id) {
        AssessmentTable t = tableMapper.selectById(id);
        if (t == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        flowLogMapper.delete(new QueryWrapper<AssessmentFlowLog>().eq("table_id", id));
        rowMapper.delete(new QueryWrapper<AssessmentRow>().eq("table_id", id));
        tableMapper.deleteById(id);
    }

    public AssessmentTableResp getDetail(Long id) {
        AssessmentTable t = getRequired(id);

        List<AssessmentRow> rows = rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()).orderByAsc("seq"));
        List<RowResp> rowResps = new ArrayList<>();
        boolean masked = perm.isRowMasked(t);
        for (AssessmentRow r : rows) {
            rowResps.add(toRowResp(r, masked));
        }
        List<FlowLogResp> logs = flowService.listLogs(t.getId());

        AssessmentTableResp resp = toTableResp(t, masked);
        resp.setRows(rowResps);
        resp.setLogs(logs);
        resp.setRealName(lookupUserName(t.getUserId()));
        resp.setDeptName(lookupDeptName(t.getDeptId()));
        resp.setDeptLeadName(lookupDeptLeadName(t.getDeptId()));
        return resp;
    }

    public Page<AssessmentTableResp> listPage(Long periodId, Long deptId, String state,
                                              int pageNo, int pageSize) {
        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        if (periodId != null) qw.eq("period_id", periodId);
        if (deptId != null) qw.eq("dept_id", deptId);
        if (StringUtils.hasText(state)) qw.eq("state", state);
        // 数据权限
        qw = perm.scopeOf(qw);
        qw.orderByDesc("id");

        Page<AssessmentTable> page = tableMapper.selectPage(
                Page.of(pageNo, pageSize), qw);

        Page<AssessmentTableResp> out = Page.of(pageNo, pageSize);
        out.setRecords(new ArrayList<>());
        out.setTotal(page.getTotal());
        for (AssessmentTable t : page.getRecords()) {
            boolean masked = perm.isRowMasked(t);
            AssessmentTableResp r = toTableResp(t, masked);
            r.setRealName(lookupUserName(t.getUserId()));
            r.setDeptName(lookupDeptName(t.getDeptId()));
            r.setDeptLeadName(lookupDeptLeadName(t.getDeptId()));
            out.getRecords().add(r);
        }
        return out;
    }

    // ==================== 状态机写动作 ====================

    @Transactional
    public void submit(Long tableId) {
        AssessmentTable t = getRequired(tableId);
        // 被考核人本人提交（EMP/部门领导/领导/专员/运营/委员），排除 HR/ADMIN
        if (!DataScopeContext.currentUserId().equals(t.getUserId())
                || !ASSESSED_ROLES.contains(perm.currentRole())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        stateMachine.assertInState(t, AssessmentState.SELF_DRAFTING);

        // 校验必填：所有 PLAN/OPEN 行需 HR 已导入指标名称且员工已填完成率（0-100）
        List<AssessmentRow> rows = rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()).orderByAsc("seq"));
        for (AssessmentRow r : rows) {
            if (RowCategory.BONUS.name().equals(r.getCategory())) continue;
            if (!StringUtils.hasText(r.getIndicatorName())) {
                throw new BizException(ResultCode.SUBMIT_REQUIRED_FIELDS,
                        "第 " + r.getSeq() + " 行指标尚未由人事导入");
            }
            if (r.getCompletionRate() == null) {
                throw new BizException(ResultCode.SUBMIT_REQUIRED_FIELDS,
                        "第 " + r.getSeq() + " 行未填写完成率");
            }
            if (r.getCompletionRate().compareTo(BigDecimal.ZERO) < 0
                    || r.getCompletionRate().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BizException(ResultCode.BAD_REQUEST,
                        "第 " + r.getSeq() + " 行完成率需在 0-100 之间");
            }
        }
        // 先重算分数，再流转状态，避免状态已变但分数未落地的中间态
        calcService.recalcByCompletionRate(t);
        // 原子条件更新：仅当仍处于 SELF_DRAFTING 才流转，防并发重复提交
        int updated = tableMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AssessmentTable>()
                .eq(AssessmentTable::getId, tableId)
                .eq(AssessmentTable::getState, AssessmentState.SELF_DRAFTING.name())
                .set(AssessmentTable::getState, AssessmentState.SELF_SUSPENDED.name())
                .set(AssessmentTable::getSubmittedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "考核表状态已变化，请刷新后重试");
        }
        t.setState(AssessmentState.SELF_SUSPENDED.name());
        t.setSubmittedAt(LocalDateTime.now());
        for (AssessmentRow r : rows) {
            r.setFrozen(true);
            rowMapper.updateById(r);
        }
        flowService.writeLog(t, AssessmentState.SELF_DRAFTING, AssessmentState.SELF_SUSPENDED, "SUBMIT");
        log.info("员工提交考核表: tableId={}, userId={}", tableId, t.getUserId());
    }

    @Transactional
    public void push(Long tableId) {
        AssessmentTable t = getRequired(tableId);
        if (!perm.isHr()) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        stateMachine.assertInState(t, AssessmentState.SELF_SUSPENDED);
        // 无部门（如公司领导）跳过部门审核，直接进入领导评分阶段
        AssessmentState target = t.getDeptId() == null ? AssessmentState.LEAD_SCORING : AssessmentState.DEPT_REVIEW;
        String action = target == AssessmentState.LEAD_SCORING ? "PUSH_DIRECT" : "PUSH";
        // 原子条件更新：仅当仍处于 SELF_SUSPENDED 才流转，防并发重复推送
        int updated = tableMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AssessmentTable>()
                .eq(AssessmentTable::getId, tableId)
                .eq(AssessmentTable::getState, AssessmentState.SELF_SUSPENDED.name())
                .set(AssessmentTable::getState, target.name())
                .set(AssessmentTable::getPushedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "考核表状态已变化，请刷新后重试");
        }
        t.setState(target.name());
        t.setPushedAt(LocalDateTime.now());
        flowService.writeLog(t, AssessmentState.SELF_SUSPENDED, target, action);
        log.info("人事推送考核表: tableId={}, 目标状态={}", tableId, target);
    }

    @Transactional
    public void approve(Long tableId, String comment) {
        AssessmentTable t = getRequired(tableId);
        if (!perm.isDeptLead() || !DataScopeContext.currentDeptId().equals(t.getDeptId())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        stateMachine.assertInState(t, AssessmentState.DEPT_REVIEW);
        // 原子条件更新：仅当仍处于 DEPT_REVIEW 才流转，防并发重复提交
        int updated = tableMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AssessmentTable>()
                .eq(AssessmentTable::getId, tableId)
                .eq(AssessmentTable::getState, AssessmentState.DEPT_REVIEW.name())
                .set(AssessmentTable::getState, AssessmentState.LEAD_SCORING.name())
                .set(AssessmentTable::getDeptApprovedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "考核表状态已变化，请刷新后重试");
        }
        t.setState(AssessmentState.LEAD_SCORING.name());
        t.setDeptApprovedAt(LocalDateTime.now());
        flowService.writeLog(t, AssessmentState.DEPT_REVIEW, AssessmentState.LEAD_SCORING, "APPROVE", comment);
        log.info("部门负责人提交考核表给领导: tableId={}, deptId={}", tableId, t.getDeptId());
    }

    @Transactional
    public void reject(Long tableId, String comment) {
        if (!StringUtils.hasText(comment)) {
            throw new BizException(ResultCode.REJECT_COMMENT_REQUIRED);
        }
        AssessmentTable t = getRequired(tableId);
        if (!perm.isDeptLead() || !DataScopeContext.currentDeptId().equals(t.getDeptId())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        stateMachine.assertInState(t, AssessmentState.DEPT_REVIEW);
        // 原子条件更新：仅当仍处于 DEPT_REVIEW 才流转
        int updated = tableMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AssessmentTable>()
                .eq(AssessmentTable::getId, tableId)
                .eq(AssessmentTable::getState, AssessmentState.DEPT_REVIEW.name())
                .set(AssessmentTable::getState, AssessmentState.SELF_DRAFTING.name()));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "考核表状态已变化，请刷新后重试");
        }
        t.setState(AssessmentState.SELF_DRAFTING.name());
        // 解冻行
        List<AssessmentRow> rows = rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()));
        for (AssessmentRow r : rows) {
            r.setFrozen(false);
            rowMapper.updateById(r);
        }
        flowService.writeLog(t, AssessmentState.DEPT_REVIEW, AssessmentState.SELF_DRAFTING, "REJECT", comment);
        log.info("部门负责人打回考核表: tableId={}, comment={}", tableId, comment);
    }

    @Transactional
    public void leadScore(Long tableId, BigDecimal leaderScore, String comment) {
        if (leaderScore == null) {
            throw new BizException(ResultCode.LEADER_SCORE_REQUIRED);
        }
        if (leaderScore.compareTo(BigDecimal.ZERO) < 0 || leaderScore.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BizException(ResultCode.SCORE_OUT_OF_RANGE);
        }
        AssessmentTable t = getRequired(tableId);
        // 评分人规则：LEAD 评非 LEAD 的表；LEAD 自己的表由绩效委员会评分
        boolean selfIsLead = t.getUserId().equals(DataScopeContext.currentUserId())
                && RoleConst.ROLE_LEAD.equals(perm.currentRole());
        if (selfIsLead) {
            throw new BizException(ResultCode.FORBIDDEN, "领导不能给自己评分，由绩效委员会评分");
        }
        if (!perm.isLead() && !RoleConst.ROLE_COMMITTEE.equals(perm.currentRole())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (RoleConst.ROLE_COMMITTEE.equals(perm.currentRole())) {
            // 委员会只能评 LEAD（公司领导）的表
            SysUser target = userMapper.selectById(t.getUserId());
            if (target == null || !RoleConst.ROLE_LEAD.equals(target.getRole())) {
                throw new BizException(ResultCode.FORBIDDEN, "绩效委员会仅可评分公司领导的考核表");
            }
        }
        stateMachine.assertInState(t, AssessmentState.LEAD_SCORING);
        // 原子条件更新：仅当仍处于 LEAD_SCORING 才流转
        int updated = tableMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AssessmentTable>()
                .eq(AssessmentTable::getId, tableId)
                .eq(AssessmentTable::getState, AssessmentState.LEAD_SCORING.name())
                .set(AssessmentTable::getState, AssessmentState.FINISHED.name())
                .set(AssessmentTable::getLeadFinishedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "考核表状态已变化，请刷新后重试");
        }
        t.setState(AssessmentState.FINISHED.name());
        t.setLeadFinishedAt(LocalDateTime.now());
        calcService.finalizeWithLeaderScore(t, leaderScore);
        flowService.writeLog(t, AssessmentState.LEAD_SCORING, AssessmentState.FINISHED, "LEAD_SCORE", comment);
        log.info("领导评分完成: tableId={}, score={}", tableId, leaderScore);
    }

    @Transactional
    public void extendSuspend(Long tableId, int days, String reason) {
        AssessmentTable t = getRequired(tableId);
        String role = perm.currentRole();
        if (!RoleConst.ROLE_PERFORMANCE_HR.equals(role) && !RoleConst.ROLE_ADMIN.equals(role)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (days <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "延长天数必须为正数");
        }
        String safeReason = StringUtils.hasText(reason) ? reason.trim() : "未填写原因";
        int next = (t.getSuspendExtendedDays() == null ? 0 : t.getSuspendExtendedDays()) + days;
        if (next > 30) {
            throw new BizException(ResultCode.EXTEND_OVER_LIMIT);
        }
        t.setSuspendExtendedDays(next);
        tableMapper.updateById(t);
        flowService.writeLog(t, AssessmentState.valueOf(t.getState()), AssessmentState.valueOf(t.getState()),
                "EXTEND_SUSPEND", "延长 " + days + " 天: " + safeReason);
        log.info("延长挂起: tableId={}, days={}, 累计={}", tableId, days, next);
    }

    // ==================== 模板初始化 ====================

    /**
     * 查询某周期某员工的考核主表。
     *
     * @param periodId 周期ID
     * @param userId   员工ID
     * @return 主表或 null
     */
    public AssessmentTable findByPeriodAndUser(Long periodId, Long userId) {
        return tableMapper.selectOne(new QueryWrapper<AssessmentTable>()
                .eq("period_id", periodId).eq("user_id", userId).last("LIMIT 1"));
    }

    /**
     * 查询某考核表全部行（按 seq 升序）。
     */
    public List<AssessmentRow> listRows(Long tableId) {
        return rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", tableId).orderByAsc("seq"));
    }

    /**
     * 更新考核行（周期导入写入明细用）。
     */
    @Transactional
    public void updateRow(AssessmentRow row) {
        rowMapper.updateById(row);
    }

    /**
     * 为指定被考核人生成考核主表 + 10 行模板。
     *
     * <p>被考核人角色：EMP / DEPT_LEAD / LEAD / DEPT_STAFF / OPERATION / COMMITTEE
     * （排除 ADMIN / PERFORMANCE_HR）。部门级角色（领导/运营/委员等）无部门时可建表，
     * 走"跳过部门审核、直接领导评分"流程。
     *
     * @param period  考核周期
     * @param userIds 参与考核的被考核人 ID 列表，为空表示全部可参与角色
     */
    @Transactional
    public void initForPeriod(AssessmentPeriod period, List<Long> userIds) {
        QueryWrapper<SysUser> qw = new QueryWrapper<SysUser>()
                .in("role", ASSESSED_ROLES)
                .eq("status", 1);
        if (userIds != null && !userIds.isEmpty()) {
            qw.in("id", userIds);
        }
        List<SysUser> users = userMapper.selectList(qw);
        for (SysUser u : users) {
            // 参与考核但无部门（如领导）允许建表；不参与考核的角色（ADMIN/HR）不在此集合
            // skip existing
            AssessmentTable exists = tableMapper.selectOne(new QueryWrapper<AssessmentTable>()
                    .eq("period_id", period.getId()).eq("user_id", u.getId()));
            if (exists != null) continue;

            AssessmentTable t = new AssessmentTable();
            t.setPeriodId(period.getId());
            t.setUserId(u.getId());
            t.setDeptId(u.getDeptId());
            t.setState(AssessmentState.SELF_DRAFTING.name());
            t.setSelfTotalScore(BigDecimal.ZERO);
            t.setSuspendExtendedDays(0);
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
        r.setFrozen(cat == RowCategory.BONUS);  // BONUS 员工不填
        rowMapper.insert(r);
    }

    // ==================== 辅助 ====================

    public AssessmentTableResp toTableResp(AssessmentTable t, boolean masked) {
        AssessmentTableResp r = new AssessmentTableResp();
        r.setId(t.getId());
        r.setPeriodId(t.getPeriodId());
        r.setUserId(t.getUserId());
        r.setDeptId(t.getDeptId());
        r.setPosition(t.getPosition());
        r.setState(t.getState());
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

        if (t.getPeriodId() != null) {
            AssessmentPeriod p = periodMapper.selectById(t.getPeriodId());
            if (p != null) r.setPeriodName(p.getName());
        }
        return r;
    }

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
        SysUser u = userMapper.selectById(uid);
        return u == null ? null : u.getRealName();
    }

    private String lookupDeptName(Long did) {
        SysDepartment d = deptMapper.selectById(did);
        return d == null ? null : d.getName();
    }

    /** 部门负责人姓名（每部门唯一的 DEPT_LEAD 角色用户，实时更新） */
    private String lookupDeptLeadName(Long deptId) {
        if (deptId == null) return null;
        SysUser lead = userMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("dept_id", deptId).eq("role", RoleConst.ROLE_DEPT_LEAD)
                .eq("status", 1).last("LIMIT 1"));
        return lead == null ? null : lead.getRealName();
    }
}
