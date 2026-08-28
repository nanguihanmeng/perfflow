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
    /** EMP / DEPT_LEAD / LEAD / ADMIN / DEPT_STAFF / OPERATION / COMMITTEE（绩效考核管理员由系统专用，admin 不可创建） */
    @NotBlank
    @Pattern(regexp = "EMP|DEPT_LEAD|LEAD|ADMIN|DEPT_STAFF|OPERATION|COMMITTEE")
    private String role;
    private Long deptId;
    @Email
    private String email;
    private String phone;
}
