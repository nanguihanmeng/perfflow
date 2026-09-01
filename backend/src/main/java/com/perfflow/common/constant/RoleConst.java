package com.perfflow.common.constant;
// 角色与状态常量。

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
