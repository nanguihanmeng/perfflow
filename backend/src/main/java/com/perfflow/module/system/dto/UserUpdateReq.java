package com.perfflow.module.system.dto;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.io.Serializable;
@Data
public class UserUpdateReq implements Serializable {

    // 真实姓名。
    private String realName;
    @Pattern(regexp = "EMP|DEPT_LEAD|LEAD|ADMIN|DEPT_STAFF|OPERATION|COMMITTEE")
    // 角色。
    private String role;
    // 部门ID。
    private Long deptId;
    // 邮箱。
    private String email;
    // 电话。
    private String phone;
    // 状态。
    private Integer status;
}
