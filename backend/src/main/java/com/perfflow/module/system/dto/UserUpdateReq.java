package com.perfflow.module.system.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateReq implements Serializable {

    private String realName;
    @Pattern(regexp = "EMP|DEPT_LEAD|LEAD|HR|ADMIN")
    private String role;
    private Long deptId;
    private String email;
    private String phone;
    private Integer status;
}
