package com.perfflow.module.home.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import com.perfflow.module.deptassessment.enums.DeptAssessmentState;
import com.perfflow.module.deptassessment.mapper.DeptAssessmentMapper;
import com.perfflow.module.home.dto.RemindersResp;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.enums.PeriodType;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 首页提醒：待办事项 + 挂起预警。
 *
 * <p>待办仅展示"进行中周期"下的考核，考核时间已结束即撤下；
 * 挂起预警仅针对进行中周期生成，已结束周期不再提示。
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final String TYPE_SUSPEND_SOON = "SUSPEND_SOON";
    private static final String BIZ_PERSONAL = "PERSONAL";
    private static final String BIZ_DEPT = "DEPT";

    /** 周期状态：进行中（HR 已开启） */
    private static final int PERIOD_STATUS_OPEN = 1;

    /** 挂起结束前提醒天数 */
    private static final long SUSPEND_WARN_DAYS = 3L;

    /** 挂起紧急提醒天数阈值 */
    private static final long SUSPEND_URGENT_DAYS = 1L;

    private final AssessmentTableMapper tableMapper;
    private final AssessmentPeriodMapper periodMapper;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final DeptAssessmentMapper deptAssessmentMapper;

    public RemindersResp load() {
        RemindersResp out = new RemindersResp();
        String role = DataScopeContext.current().getPrimaryRole();
        Long currentUid = DataScopeContext.currentUserId();
        Long currentDeptId = DataScopeContext.currentDeptId();

        List<AssessmentPeriod> activePeriods = periodMapper.selectList(
                new QueryWrapper<AssessmentPeriod>().eq("status", PERIOD_STATUS_OPEN));
        Map<Long, String> periodNames = new HashMap<>(activePeriods.size() * 2);
        List<Long> activePeriodIds = new ArrayList<>(activePeriods.size());
        for (AssessmentPeriod p : activePeriods) {
            activePeriodIds.add(p.getId());
            periodNames.put(p.getId(), p.getName());
        }

        Set<Long> seenPersonal = new HashSet<>();
        Set<Long> seenDept = new HashSet<>();
        loadPersonalTodos(role, currentUid, currentDeptId, activePeriodIds, periodNames, out, seenPersonal);
        loadDeptTodos(role, currentDeptId, activePeriods, out, seenDept);
        loadSuspendWarnings(role, activePeriods, out);
        return out;
    }

    /** 个人考核线待办：本人被考核事项 + 各角色审核/评分职责内事项，仅限进行中周期 */
    private void loadPersonalTodos(String role, Long currentUid, Long currentDeptId,
                                   List<Long> activePeriodIds, Map<Long, String> periodNames,
                                   RemindersResp out, Set<Long> seen) {
        if (activePeriodIds.isEmpty()) {
            return;
        }
        boolean hasCondition = false;
        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        switch (role == null ? "" : role) {
            case RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION,
                 RoleConst.ROLE_COMMITTEE -> {
                hasCondition = true;
                if (currentUid != null) {
                    qw.eq("user_id", currentUid);
                }
                qw.in("state", AssessmentState.SELF_DRAFTING.name(),
                        AssessmentState.SELF_SUSPENDED.name(),
                        AssessmentState.LEAD_SCORING.name());
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
                hasCondition = true;
                if (currentUid != null) {
                    qw.eq("user_id", currentUid);
                }
                qw.in("state", AssessmentState.SELF_DRAFTING.name(),
                        AssessmentState.SELF_SUSPENDED.name(),
                        AssessmentState.LEAD_SCORING.name());
                appendDeptReviewTodos(currentDeptId, activePeriodIds, periodNames, out, seen);
            }
            case RoleConst.ROLE_LEAD -> {
                hasCondition = true;
                qw.in("state", AssessmentState.LEAD_SCORING.name());
            }
            case RoleConst.ROLE_PERFORMANCE_HR -> {
                hasCondition = true;
                qw.in("state", AssessmentState.SELF_SUSPENDED.name());
            }
            default -> { /* ADMIN 无业务可见性 */ }
        }
        if (!hasCondition) {
            return;
        }
        qw.in("period_id", activePeriodIds);
        qw.orderByAsc("id");
        appendPersonalReminders(tableMapper.selectList(qw), periodNames, out, seen);
    }

    /** 部门负责人审核个人考核职责：本部门待审核（DEPT_REVIEW）的表 */
    private void appendDeptReviewTodos(Long currentDeptId, List<Long> activePeriodIds,
                                       Map<Long, String> periodNames, RemindersResp out, Set<Long> seen) {
        if (currentDeptId == null || activePeriodIds.isEmpty()) {
            return;
        }
        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        qw.eq("dept_id", currentDeptId);
        qw.eq("state", AssessmentState.DEPT_REVIEW.name());
        qw.in("period_id", activePeriodIds);
        qw.orderByAsc("id");
        appendPersonalReminders(tableMapper.selectList(qw), periodNames, out, seen);
    }

    /** 部门考核线待办：各角色按职责状态筛选，仅限进行中部门线周期 */
    private void loadDeptTodos(String role, Long currentDeptId, List<AssessmentPeriod> activePeriods,
                               RemindersResp out, Set<Long> seen) {
        String roleSafe = role == null ? "" : role;
        List<Integer> statusList;
        switch (roleSafe) {
            case RoleConst.ROLE_DEPT_STAFF -> {
                if (currentDeptId == null) {
                    return;
                }
                statusList = Collections.singletonList(DeptAssessmentState.SELF_FILLING.getCode());
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
                if (currentDeptId == null) {
                    return;
                }
                statusList = Collections.singletonList(DeptAssessmentState.PENDING_REVIEW.getCode());
            }
            case RoleConst.ROLE_OPERATION ->
                    statusList = Collections.singletonList(DeptAssessmentState.PENDING_AUDIT.getCode());
            case RoleConst.ROLE_COMMITTEE ->
                    statusList = Collections.singletonList(DeptAssessmentState.PENDING_APPROVE.getCode());
            default -> {
                return;
            }
        }
        boolean deptScoped = RoleConst.ROLE_DEPT_STAFF.equals(roleSafe) || RoleConst.ROLE_DEPT_LEAD.equals(roleSafe);
        for (AssessmentPeriod p : activePeriods) {
            if (!isDeptLinePeriod(p)) {
                continue;
            }
            QueryWrapper<DeptAssessment> qw = new QueryWrapper<>();
            qw.eq("period_id", p.getId());
            qw.in("status", statusList);
            if (deptScoped && currentDeptId != null) {
                qw.eq("dept_id", currentDeptId);
            }
            appendDeptReminders(p, deptAssessmentMapper.selectList(qw), out, seen);
        }
    }

    /** 挂起预警：仅进行中周期且 HR/部门负责人可见，已结束周期不提示 */
    private void loadSuspendWarnings(String role, List<AssessmentPeriod> activePeriods, RemindersResp out) {
        String roleSafe = role == null ? "" : role;
        boolean visible = RoleConst.ROLE_PERFORMANCE_HR.equals(roleSafe) || RoleConst.ROLE_DEPT_LEAD.equals(roleSafe);
        if (!visible) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (AssessmentPeriod p : activePeriods) {
            if (p.getSuspendEndDate() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, p.getSuspendEndDate());
            if (days < 0 || days > SUSPEND_WARN_DAYS) {
                continue;
            }
            RemindersResp.Reminder r = new RemindersResp.Reminder();
            r.setBizType(TYPE_SUSPEND_SOON);
            r.setType(TYPE_SUSPEND_SOON);
            r.setTargetPeriodId(p.getId());
            r.setTitle("周期 " + p.getName() + " 自评挂起将在 " + days + " 天后结束");
            r.setDescription("请及时确认推送或延长挂起时间");
            r.setSeverity(days <= SUSPEND_URGENT_DAYS ? 3 : 2);
            out.getUpcomingSuspends().add(r);
        }
    }

    /** 组装个人考核待办提醒（标题含"谁、什么事"，便于不点进去即可识别） */
    private void appendPersonalReminders(List<AssessmentTable> tables, Map<Long, String> periodNames,
                                         RemindersResp out, Set<Long> seen) {
        if (tables == null || tables.isEmpty()) {
            return;
        }
        Map<Long, String> userNames = loadUserNames(tables);
        Map<Long, String> deptNames = loadDeptNames(tables);
        for (AssessmentTable t : tables) {
            if (!seen.add(t.getId())) {
                continue;
            }
            RemindersResp.Reminder r = new RemindersResp.Reminder();
            r.setBizType(BIZ_PERSONAL);
            r.setType(t.getState());
            r.setTargetTableId(t.getId());
            r.setTargetPeriodId(t.getPeriodId());
            String who = buildWho(userNames.get(t.getUserId()), deptNames.get(t.getDeptId()));
            String action = stateText(t.getState());
            r.setTitle(action + who);
            r.setDescription(periodNames.getOrDefault(t.getPeriodId(), "") + "，" + action);
            r.setSeverity(AssessmentState.SELF_SUSPENDED.name().equals(t.getState()) ? 2 : 1);
            out.getTodos().add(r);
        }
    }

    /** 组装部门考核待办提醒（标题含部门名与动作） */
    private void appendDeptReminders(AssessmentPeriod period, List<DeptAssessment> list,
                                     RemindersResp out, Set<Long> seen) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> deptNames = loadDeptNamesByDeptIds(list);
        for (DeptAssessment d : list) {
            if (!seen.add(d.getId())) {
                continue;
            }
            String action = deptStateText(d.getStatus());
            RemindersResp.Reminder r = new RemindersResp.Reminder();
            r.setBizType(BIZ_DEPT);
            r.setType(deptStateType(d.getStatus()));
            r.setTargetAssessmentId(d.getId());
            r.setTargetPeriodId(period.getId());
            r.setTitle(action + " — " + deptNames.getOrDefault(d.getDeptId(), "未知部门"));
            r.setDescription(period.getName() + "，" + action);
            r.setSeverity(1);
            out.getTodos().add(r);
        }
    }

    /** 批量查询被考核人姓名 */
    private Map<Long, String> loadUserNames(List<AssessmentTable> tables) {
        Set<Long> userIds = new HashSet<>();
        for (AssessmentTable t : tables) {
            if (t.getUserId() != null) {
                userIds.add(t.getUserId());
            }
        }
        Map<Long, String> names = new HashMap<>(userIds.size() * 2);
        if (userIds.isEmpty()) {
            return names;
        }
        for (SysUser u : userMapper.selectBatchIds(userIds)) {
            names.put(u.getId(), u.getRealName());
        }
        return names;
    }

    /** 批量查询个人考核表所在部门名 */
    private Map<Long, String> loadDeptNames(List<AssessmentTable> tables) {
        Set<Long> deptIds = new HashSet<>();
        for (AssessmentTable t : tables) {
            if (t.getDeptId() != null) {
                deptIds.add(t.getDeptId());
            }
        }
        Map<Long, String> names = new HashMap<>(deptIds.size() * 2);
        if (deptIds.isEmpty()) {
            return names;
        }
        for (SysDepartment d : deptMapper.selectBatchIds(deptIds)) {
            names.put(d.getId(), d.getName());
        }
        return names;
    }

    /** 批量查询部门考核涉及的部门名 */
    private Map<Long, String> loadDeptNamesByDeptIds(List<DeptAssessment> list) {
        Set<Long> deptIds = new HashSet<>();
        for (DeptAssessment d : list) {
            if (d.getDeptId() != null) {
                deptIds.add(d.getDeptId());
            }
        }
        Map<Long, String> names = new HashMap<>(deptIds.size() * 2);
        if (deptIds.isEmpty()) {
            return names;
        }
        for (SysDepartment d : deptMapper.selectBatchIds(deptIds)) {
            names.put(d.getId(), d.getName());
        }
        return names;
    }

    private String buildWho(String realName, String deptName) {
        if (realName == null && deptName == null) {
            return "";
        }
        if (realName == null) {
            return " — " + deptName;
        }
        if (deptName == null) {
            return " — " + realName;
        }
        return " — " + realName + "（" + deptName + "）";
    }

    private boolean isDeptLinePeriod(AssessmentPeriod period) {
        if (period.getPeriodType() == null) {
            return false;
        }
        try {
            return PeriodType.of(period.getPeriodType()).isDeptLine();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String stateText(String s) {
        return switch (s) {
            case "SELF_DRAFTING" -> "请继续填报自评";
            case "SELF_SUSPENDED" -> "等待人事确认推送";
            case "DEPT_REVIEW" -> "请部门领导审核";
            case "LEAD_SCORING" -> "领导评分中";
            case "FINISHED" -> "已完成";
            default -> s;
        };
    }

    private String deptStateText(Integer status) {
        if (status == null) {
            return "部门考核";
        }
        if (DeptAssessmentState.SELF_FILLING.getCode() == status) {
            return "请填报部门考核";
        }
        if (DeptAssessmentState.PENDING_REVIEW.getCode() == status) {
            return "请复核部门考核";
        }
        if (DeptAssessmentState.PENDING_AUDIT.getCode() == status) {
            return "请初审部门考核";
        }
        if (DeptAssessmentState.PENDING_APPROVE.getCode() == status) {
            return "请审批部门考核";
        }
        if (DeptAssessmentState.COMPLETED.getCode() == status) {
            return "部门考核已完成";
        }
        return "部门考核未开始";
    }

    /** 部门考核提醒类型：直接复用状态编码字符串，供前端按业务区分 */
    private String deptStateType(Integer status) {
        return status == null ? "DEPT" : "DEPT_" + status;
    }
}
