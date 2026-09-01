package com.perfflow.module.auth.dto;
import lombok.Data;
import java.io.Serializable;
@Data
public class LoginResp implements Serializable {
    // 访问令牌。
    private String accessToken;
    // 刷新令牌。
    private String refreshToken;
    // 过期秒数。
    private long expiresIn;
    private String tokenType = "Bearer";
    // 用户ID。
    private Long userId;
    // 登录名。
    private String username;
    // 真实姓名。
    private String realName;
    // 角色。
    private String role;
    // 部门ID。
    private Long deptId;
    // 部门名。
    private String deptName;
    // 是否部门负责人。
    private Boolean deptLead;
    // 是否必须改密。
    private Boolean mustChangePassword;
}
