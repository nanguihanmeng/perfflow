package com.perfflow.module.system.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.io.Serializable;
@Data
public class UserCreateReq implements Serializable {

    @NotBlank
    // 登录名。
    private String username;
    @NotBlank
    // 真实姓名。
    private String realName;
    @NotBlank
    @Pattern(regexp = "EMP|DEPT_LEAD|LEAD|ADMIN|DEPT_STAFF|OPERATION|COMMITTEE")
    // 角色。
    private String role;
    // 部门ID。
    private Long deptId;
    @Email
    // 邮箱。
    private String email;
    // 电话。
    private String phone;
}
