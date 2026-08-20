package com.perfflow.module.auth.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginResp implements Serializable {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private Long deptId;
    private String deptName;
    private Boolean deptLead;
    private Boolean mustChangePassword;
}
