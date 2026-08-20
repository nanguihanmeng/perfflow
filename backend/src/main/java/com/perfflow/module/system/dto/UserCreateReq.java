package com.perfflow.module.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserCreateReq implements Serializable {

    @NotBlank
    private String username;
    @NotBlank
    private String realName;
    /** EMP / DEPT_LEAD / LEAD / HR / ADMIN */
    @NotBlank
    @Pattern(regexp = "EMP|DEPT_LEAD|LEAD|HR|ADMIN")
    private String role;
    private Long deptId;
    private Boolean deptLead;
    @Email
    private String email;
    private String phone;
}
