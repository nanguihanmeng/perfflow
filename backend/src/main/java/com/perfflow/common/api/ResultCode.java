package com.perfflow.common.api;
import lombok.Getter;
// 业务码枚举。
@Getter
public enum ResultCode {

    // SUCCESS
    SUCCESS(0, "ok"),
    // ---- 1000：通用 ----
    BAD_REQUEST(1001, "请求参数不合法"),
    // UNAUTHORIZED
    UNAUTHORIZED(1002, "请先登录"),
    // FORBIDDEN
    FORBIDDEN(1003, "无权访问"),
    // NOT_FOUND
    NOT_FOUND(1004, "资源不存在"),
    // METHOD_NOT_ALLOWED
    METHOD_NOT_ALLOWED(1005, "请求方式不被支持"),
    // VALIDATION_FAILED
    VALIDATION_FAILED(1006, "参数校验未通过"),
    // ADMIN_NO_BUSINESS_VISIBILITY
    ADMIN_NO_BUSINESS_VISIBILITY(1007, "系统管理员账号不可访问业务数据"),
    // ---- 1100：登录/鉴权 ----
    LOGIN_INVALID(1101, "用户名或密码错误"),
    // ACCOUNT_DISABLED
    ACCOUNT_DISABLED(1102, "账号已被禁用"),
    // TOKEN_INVALID
    TOKEN_INVALID(1103, "登录凭证无效或已过期"),
    // TOKEN_EXPIRED
    TOKEN_EXPIRED(1104, "登录凭证已过期，请重新登录"),
    // MUST_CHANGE_PASSWORD_INIT
    MUST_CHANGE_PASSWORD_INIT(1105, "首次登录必须修改密码"),
    // INIT_PASSWORD
    INIT_PASSWORD(1106, "请设置新密码"),
    // ACCOUNT_LOCKED
    ACCOUNT_LOCKED(1107, "登录失败次数过多，账号已临时锁定，请稍后再试"),
    // ---- 2000：业务规则 ----
    PERIOD_NOT_OPEN(2001, "考核周期未开启"),
    // PERMISSION_DENIED_FOR_ROW
    PERMISSION_DENIED_FOR_ROW(2002, "无可操作权限"),
    // STATE_NOT_ALLOWED
    STATE_NOT_ALLOWED(2010, "当前状态不允许该操作"),
    // SUBMIT_REQUIRED_FIELDS
    SUBMIT_REQUIRED_FIELDS(2011, "请完成所有行的填写后再提交"),
    // REJECT_COMMENT_REQUIRED
    REJECT_COMMENT_REQUIRED(2012, "打回时必须填写原因"),
    // ADJUST_REMARK_REQUIRED
    ADJUST_REMARK_REQUIRED(2013, "调分必须填写原因"),
    // LEADER_SCORE_REQUIRED
    LEADER_SCORE_REQUIRED(2020, "请填写领导评分"),
    // SCORE_OUT_OF_RANGE
    SCORE_OUT_OF_RANGE(2021, "分数必须在 0-100 之间"),
    // EXTEND_OVER_LIMIT
    EXTEND_OVER_LIMIT(2030, "累计延长挂起天数已达上限（30 天）"),
    // ---- 2040：部门考核 ----
    DEPT_ASSESS_NOT_FOUND(2040, "部门考核表不存在"),
    // DEPT_ASSESS_ALREADY_SUBMITTED
    DEPT_ASSESS_ALREADY_SUBMITTED(2041, "部门考核已提交，不可重复提交"),
    // DEPT_KPI_REQUIRED
    DEPT_KPI_REQUIRED(2042, "请完整填写 KPI 指标"),
    // ---- 2050：等级配额 ----
    GRADE_QUOTA_NOT_FOUND(2050, "未找到等级配额配置"),
    // GRADE_QUOTA_EXISTS
    GRADE_QUOTA_EXISTS(2051, "该部门等级与员工层级的配额配置已存在"),
    // ---- 2060：权重配置 ----
    WEIGHT_CONFIG_NOT_FOUND(2060, "未找到权重配置"),
    // ---- 2070：监控看板 ----
    REMIND_TARGET_REQUIRED(2070, "催办对象不能为空"),
    // ---- 3000：系统 ----
    INTERNAL_ERROR(3000, "系统开小差，请稍后再试");
    // 编码
    private final long code;
    // 消息
    private final String message;

    ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }
}
