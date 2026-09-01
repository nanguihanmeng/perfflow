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
//
 // 首页提醒服务：待办事项 + 挂起预警。
 //

 // 挂起预警仅针对进行中周期生成，已结束周期不再提示。
 //
@Service
@RequiredArgsConstructor
public class HomeService {

    //
    private static final String TYPE_SUSPEND_SOON = "SUSPEND_SOON";
    //
    private static final String BIZ_PERSONAL = "PERSONAL";
    //
    private static final String BIZ_DEPT = "DEPT";
    //
    private static final int SEVERITY_INFO = 1;
    private static final int SEVERITY_WARN = 2;
    private static final int SEVERITY_URGENT = 3;
    //
    private static final int PERIOD_STATUS_OPEN = 1;
    //
    private static final long SUSPEND_WARN_DAYS = 3L;
    //
    private static final long SUSPEND_URGENT_DAYS = 1L;
    private final AssessmentTableMapper tableMapper;
    private final AssessmentPeriodMapper periodMapper;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final DeptAssessmentMapper deptAssessmentMapper;
    //

     //

     //
    public RemindersResp load() {
        // 构造返回结构并取当前用户上下文
        RemindersResp out = new RemindersResp();
        // 取当前用户上下文
        String role = DataScopeContext.current().getPrimaryRole();
        // 取当前用户上下文
        Long currentUid = DataScopeContext.currentUserId();
        // 取当前用户上下文
        Long currentDeptId = DataScopeContext.currentDeptId();
        // 仅查询进行中周期，用于过滤待办与挂起预警
        List<AssessmentPeriod> activePeriods = periodMapper.selectList(
                new QueryWrapper<AssessmentPeriod>().eq("status", PERIOD_STATUS_OPEN));
        // 建立周期ID到名称的映射，供待办描述使用
        Map<Long, String> periodNames = new HashMap<>(activePeriods.size() * 2);
        // 构建集合容器
        List<Long> activePeriodIds = new ArrayList<>(activePeriods.size());
        for (AssessmentPeriod p : activePeriods) {
            activePeriodIds.add(p.getId());
            periodNames.put(p.getId(), p.getName());
        }

        // 分别加载个人线待办、部门线待办与挂起预警
        Set<Long> seenPersonal = new HashSet<>();
        // 构建集合容器
        Set<Long> seenDept = new HashSet<>();
        // 加载待办/预警
        loadPersonalTodos(role, currentUid, currentDeptId, activePeriodIds, periodNames, out, seenPersonal);
        // 加载待办/预警
        loadDeptTodos(role, currentDeptId, activePeriods, out, seenDept);
        // 加载待办/预警
        loadSuspendWarnings(role, activePeriods, out);
        // 返回结果
        return out;
    }

    //

     //
    private void loadPersonalTodos(String role, Long currentUid, Long currentDeptId,
                                   // 构建集合容器
                                   List<Long> activePeriodIds, Map<Long, String> periodNames,
                                   RemindersResp out, Set<Long> seen) {
        // 无进行中周期直接返回
        if (activePeriodIds.isEmpty()) {
            return;
        }
        boolean hasCondition = false;
        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        // 按角色分发：员工/专员/运营/委员会看本人事项，负责人额外看本部门待审核
        switch (role == null ? "" : role) {
            case RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION,
                 RoleConst.ROLE_COMMITTEE -> {
                hasCondition = true;
                // 限定本人被考核事项
                if (currentUid != null) {
                    qw.eq("user_id", currentUid);
                }
                qw.in("state", AssessmentState.SELF_DRAFTING.name(),
                        AssessmentState.SELF_SUSPENDED.name(),
                        AssessmentState.LEAD_SCORING.name());
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
                hasCondition = true;
                // 本人被考核事项 + 本部门待审核事项
                if (currentUid != null) {
                    qw.eq("user_id", currentUid);
                }
                qw.in("state", AssessmentState.SELF_DRAFTING.name(),
                        AssessmentState.SELF_SUSPENDED.name(),
                        AssessmentState.LEAD_SCORING.name());
                // 组装提醒
                appendDeptReviewTodos(currentDeptId, activePeriodIds, periodNames, out, seen);
            }
            case RoleConst.ROLE_LEAD -> {
                hasCondition = true;
                // 领导看全部待评分表
                qw.in("state", AssessmentState.LEAD_SCORING.name());
            }
            case RoleConst.ROLE_PERFORMANCE_HR -> {
                hasCondition = true;
                // HR 看全部待推送挂起表
                qw.in("state", AssessmentState.SELF_SUSPENDED.name());
            }
            default -> { /* ADMIN 无业务可见性 */ }
        }
        // 条件分支
        if (!hasCondition) {
            return;
        }
        // 限定进行中周期并组装提醒
        qw.in("period_id", activePeriodIds);
        qw.orderByAsc("id");
        // 查询列表
        appendPersonalReminders(tableMapper.selectList(qw), periodNames, out, seen);
    }

    //

     //
    private void appendDeptReviewTodos(Long currentDeptId, List<Long> activePeriodIds,
                                       // 构建集合容器
                                       Map<Long, String> periodNames, RemindersResp out, Set<Long> seen) {
        // 判空处理
        if (currentDeptId == null || activePeriodIds.isEmpty()) {
            return;
        }
        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        qw.eq("dept_id", currentDeptId);
        qw.eq("state", AssessmentState.DEPT_REVIEW.name());
        qw.in("period_id", activePeriodIds);
        qw.orderByAsc("id");
        // 查询列表
        appendPersonalReminders(tableMapper.selectList(qw), periodNames, out, seen);
    }

    //

     //
    private void loadDeptTodos(String role, Long currentDeptId, List<AssessmentPeriod> activePeriods,
                               RemindersResp out, Set<Long> seen) {
        String roleSafe = role == null ? "" : role;
        // 构建集合容器
        List<Integer> statusList;
        // 按角色取对应待办状态：专员看自评中、负责人看待复核、运营看待初审、委员会看待审批
        switch (roleSafe) {
            case RoleConst.ROLE_DEPT_STAFF -> {
                // 判空处理
                if (currentDeptId == null) {
                    return;
                }
                statusList = Collections.singletonList(DeptAssessmentState.SELF_FILLING.getCode());
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
                // 判空处理
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
        // 遍历进行中的部门线周期，查待办考核并组装提醒
        for (AssessmentPeriod p : activePeriods) {
            // 条件分支
            if (!isDeptLinePeriod(p)) {
                continue;
            }
            QueryWrapper<DeptAssessment> qw = new QueryWrapper<>();
            qw.eq("period_id", p.getId());
            qw.in("status", statusList);
            // 非空才处理
            if (deptScoped && currentDeptId != null) {
                qw.eq("dept_id", currentDeptId);
            }
            // 查询列表
            appendDeptReminders(p, deptAssessmentMapper.selectList(qw), out, seen);
        }
    }

    //
     // 加载挂起预警：仅进行中周期且 HR/部门负责人可见。
     //
    private void loadSuspendWarnings(String role, List<AssessmentPeriod> activePeriods, RemindersResp out) {
        String roleSafe = role == null ? "" : role;
        boolean visible = RoleConst.ROLE_PERFORMANCE_HR.equals(roleSafe) || RoleConst.ROLE_DEPT_LEAD.equals(roleSafe);
        // 条件分支
        if (!visible) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (AssessmentPeriod p : activePeriods) {
            // 判空处理
            if (p.getSuspendEndDate() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, p.getSuspendEndDate());
            // 仅在结束前 3 天内预警
            if (days < 0 || days > SUSPEND_WARN_DAYS) {
                continue;
            }
            RemindersResp.Reminder r = new RemindersResp.Reminder();
            r.setBizType(TYPE_SUSPEND_SOON);
            r.setType(TYPE_SUSPEND_SOON);
            r.setTargetPeriodId(p.getId());
            r.setTitle("周期 " + p.getName() + " 自评挂起将在 " + days + " 天后结束");
            r.setDescription("请及时确认推送或延长挂起时间");
            r.setSeverity(days <= SUSPEND_URGENT_DAYS ? SEVERITY_URGENT : SEVERITY_WARN);
            out.getUpcomingSuspends().add(r);
        }
    }

    //
     // 组装个人考核待办提醒，标题含"谁、什么事"便于直接识别。
     //
    private void appendPersonalReminders(List<AssessmentTable> tables, Map<Long, String> periodNames,
                                         RemindersResp out, Set<Long> seen) {
        // 判空处理
        if (tables == null || tables.isEmpty()) {
            return;
        }
        // 批量加载姓名与部门名，避免 N+1
        Map<Long, String> userNames = loadUserNames(tables);
        // 构建集合容器
        Map<Long, String> deptNames = loadDeptNames(tables);
        for (AssessmentTable t : tables) {
            // 条件分支
            if (!seen.add(t.getId())) {
                continue;
            }
            RemindersResp.Reminder r = new RemindersResp.Reminder();
            r.setBizType(BIZ_PERSONAL);
            r.setType(t.getState());
            r.setTargetTableId(t.getId());
            r.setTargetPeriodId(t.getPeriodId());
            // 文案处理
            String who = buildWho(userNames.get(t.getUserId()), deptNames.get(t.getDeptId()));
            // 文案处理
            String action = stateText(t.getState());
            r.setTitle(action + who);
            r.setDescription(periodNames.getOrDefault(t.getPeriodId(), "") + "，" + action);
            r.setSeverity(AssessmentState.SELF_SUSPENDED.name().equals(t.getState())
                    ? SEVERITY_WARN : SEVERITY_INFO);
            out.getTodos().add(r);
        }
    }

    //
     // 组装部门考核待办提醒，标题含部门名与动作。
     //
    private void appendDeptReminders(AssessmentPeriod period, List<DeptAssessment> list,
                                     RemindersResp out, Set<Long> seen) {
        // 判空处理
        if (list == null || list.isEmpty()) {
            return;
        }
        // 构建集合容器
        Map<Long, String> deptNames = loadDeptNamesByDeptIds(list);
        for (DeptAssessment d : list) {
            // 条件分支
            if (!seen.add(d.getId())) {
                continue;
            }
            // 文案处理
            String action = deptStateText(d.getStatus());
            RemindersResp.Reminder r = new RemindersResp.Reminder();
            r.setBizType(BIZ_DEPT);
            // 文案处理
            r.setType(deptStateType(d.getStatus()));
            r.setTargetAssessmentId(d.getId());
            r.setTargetPeriodId(period.getId());
            r.setTitle(action + " — " + deptNames.getOrDefault(d.getDeptId(), "未知部门"));
            r.setDescription(period.getName() + "，" + action);
            r.setSeverity(SEVERITY_INFO);
            out.getTodos().add(r);
        }
    }

    //
    private Map<Long, String> loadUserNames(List<AssessmentTable> tables) {
        // 构建集合容器
        Set<Long> userIds = new HashSet<>();
        for (AssessmentTable t : tables) {
            // 非空才处理
            if (t.getUserId() != null) {
                userIds.add(t.getUserId());
            }
        }
        // 构建集合容器
        Map<Long, String> names = new HashMap<>(userIds.size() * 2);
        // 条件分支
        if (userIds.isEmpty()) {
            return names;
        }
        // 系统数据访问
        for (SysUser u : userMapper.selectBatchIds(userIds)) {
            names.put(u.getId(), u.getRealName());
        }
        return names;
    }

    //
    private Map<Long, String> loadDeptNames(List<AssessmentTable> tables) {
        // 构建集合容器
        Set<Long> deptIds = new HashSet<>();
        for (AssessmentTable t : tables) {
            // 非空才处理
            if (t.getDeptId() != null) {
                deptIds.add(t.getDeptId());
            }
        }
        // 构建集合容器
        Map<Long, String> names = new HashMap<>(deptIds.size() * 2);
        // 条件分支
        if (deptIds.isEmpty()) {
            return names;
        }
        // 系统数据访问
        for (SysDepartment d : deptMapper.selectBatchIds(deptIds)) {
            names.put(d.getId(), d.getName());
        }
        return names;
    }

    //
    private Map<Long, String> loadDeptNamesByDeptIds(List<DeptAssessment> list) {
        // 构建集合容器
        Set<Long> deptIds = new HashSet<>();
        for (DeptAssessment d : list) {
            // 非空才处理
            if (d.getDeptId() != null) {
                deptIds.add(d.getDeptId());
            }
        }
        // 构建集合容器
        Map<Long, String> names = new HashMap<>(deptIds.size() * 2);
        // 条件分支
        if (deptIds.isEmpty()) {
            return names;
        }
        // 系统数据访问
        for (SysDepartment d : deptMapper.selectBatchIds(deptIds)) {
            names.put(d.getId(), d.getName());
        }
        return names;
    }

    //
    private String buildWho(String realName, String deptName) {
        // 判空处理
        if (realName == null && deptName == null) {
            return "";
        }
        // 判空处理
        if (realName == null) {
            return " — " + deptName;
        }
        // 判空处理
        if (deptName == null) {
            return " — " + realName;
        }
        return " — " + realName + "（" + deptName + "）";
    }

    //
    private boolean isDeptLinePeriod(AssessmentPeriod period) {
        // 判空处理
        if (period.getPeriodType() == null) {
            return false;
        }
        try {
            return PeriodType.of(period.getPeriodType()).isDeptLine();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    //
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

    //
    private String deptStateText(Integer status) {
        // 判空处理
        if (status == null) {
            return "部门考核";
        }
        // 值比较
        if (DeptAssessmentState.SELF_FILLING.getCode() == status) {
            return "请填报部门考核";
        }
        // 值比较
        if (DeptAssessmentState.PENDING_REVIEW.getCode() == status) {
            return "请复核部门考核";
        }
        // 值比较
        if (DeptAssessmentState.PENDING_AUDIT.getCode() == status) {
            return "请初审部门考核";
        }
        // 值比较
        if (DeptAssessmentState.PENDING_APPROVE.getCode() == status) {
            return "请审批部门考核";
        }
        // 值比较
        if (DeptAssessmentState.COMPLETED.getCode() == status) {
            return "部门考核已完成";
        }
        return "部门考核未开始";
    }

    //
    private String deptStateType(Integer status) {
        return status == null ? "DEPT" : "DEPT_" + status;
    }
}
