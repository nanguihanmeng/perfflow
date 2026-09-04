package com.perfflow.common.constant;
// 角色与状态常量。

import java.util.List;
import java.util.Set;

public final class RoleConst {

    private RoleConst() {}

    // ---- 角色编码 ----
    public static final String ROLE_EMP          = "EMP";
    public static final String ROLE_DEPT_LEAD    = "DEPT_LEAD";
    public static final String ROLE_LEAD         = "LEAD";
    public static final String ROLE_PERFORMANCE_HR = "PERFORMANCE_HR";
    public static final String ROLE_ADMIN        = "ADMIN";
    public static final String ROLE_DEPT_STAFF   = "DEPT_STAFF";
    public static final String ROLE_OPERATION    = "OPERATION";
    public static final String ROLE_COMMITTEE    = "COMMITTEE";
    // ---- 全部角色（新增角色登记于此，默认即参与被考核）----
    public static final List<String> ALL_ROLES = List.of(
            ROLE_EMP, ROLE_DEPT_LEAD, ROLE_LEAD,
            ROLE_PERFORMANCE_HR, ROLE_ADMIN,
            ROLE_DEPT_STAFF, ROLE_OPERATION, ROLE_COMMITTEE);
    // ---- 只行使管理/审核、不参与被考核的角色 ----
    private static final Set<String> NON_ASSESSED_ROLES = Set.of(
            ROLE_ADMIN, ROLE_PERFORMANCE_HR);

    // ---- 参与考核的角色 = 全部角色 − 非被考核角色 ----
    // 新增角色：登记进 ALL_ROLES 即自动参与被考核，无需再改这里；
    // 仅当新角色属"只管理不参与被考核"时，再额外加进 NON_ASSESSED_ROLES 即可。
    public static final List<String> ASSESSED_ROLES = ALL_ROLES.stream()
            .filter(role -> !NON_ASSESSED_ROLES.contains(role))
            .toList();
    // ---- Spring Security role prefix ----
    public static final String SEC_EMP           = "ROLE_EMP";
    public static final String SEC_DEPT_LEAD     = "ROLE_DEPT_LEAD";
    public static final String SEC_LEAD          = "ROLE_LEAD";
    public static final String SEC_PERFORMANCE_HR = "ROLE_PERFORMANCE_HR";
    public static final String SEC_ADMIN         = "ROLE_ADMIN";
    public static final String SEC_DEPT_STAFF    = "ROLE_DEPT_STAFF";
    public static final String SEC_OPERATION     = "ROLE_OPERATION";
    public static final String SEC_COMMITTEE     = "ROLE_COMMITTEE";
    // ---- 业务接口前缀 ----
    public static final String ADMIN_PREFIX    = "/admin/";
    public static final String ASSESS_PREFIX   = "/assessment-tables";
    public static final String PERIOD_PREFIX   = "/periods";
    public static final String HOME_PREFIX     = "/home/";
    public static final String AUTH_PREFIX     = "/auth/";
    public static final String DEPT_ASSESS_PREFIX = "/dept-assessments";
    public static final String MONITOR_PREFIX  = "/monitor";
    public static final String NOTIFICATION_PREFIX = "/notifications";
    public static final String AUDIT_PREFIX    = "/audit-logs";
}
