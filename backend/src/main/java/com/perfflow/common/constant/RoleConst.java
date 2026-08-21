package com.perfflow.common.constant;

/**
 * 角色与状态常量。
 */
public final class RoleConst {

    private RoleConst() {}

    // ---- 角色编码 ----
    public static final String ROLE_EMP          = "EMP";
    public static final String ROLE_DEPT_LEAD    = "DEPT_LEAD";
    public static final String ROLE_LEAD         = "LEAD";
    /** 绩效考核管理员：发布周期、收集/导入考核表、导出（最高权限，不参与考核，账号不归个人） */
    public static final String ROLE_PERFORMANCE_HR = "PERFORMANCE_HR";
    public static final String ROLE_ADMIN        = "ADMIN";

    // ---- Spring Security role prefix ----
    public static final String SEC_EMP           = "ROLE_EMP";
    public static final String SEC_DEPT_LEAD     = "ROLE_DEPT_LEAD";
    public static final String SEC_LEAD          = "ROLE_LEAD";
    public static final String SEC_PERFORMANCE_HR = "ROLE_PERFORMANCE_HR";
    public static final String SEC_ADMIN         = "ROLE_ADMIN";

    // ---- 业务接口前缀 ----
    public static final String ADMIN_PREFIX    = "/admin/";
    public static final String ASSESS_PREFIX   = "/assessment-tables";
    public static final String PERIOD_PREFIX   = "/periods";
    public static final String HOME_PREFIX     = "/home/";
    public static final String AUTH_PREFIX     = "/auth/";
}
