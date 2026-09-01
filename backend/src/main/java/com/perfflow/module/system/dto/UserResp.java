package com.perfflow.module.system.dto;
import com.perfflow.module.system.entity.SysUser;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
@Data
public class UserResp implements Serializable {
    // ID。
    private Long id;
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
    // 邮箱。
    private String email;
    // 电话。
    private String phone;
    // 状态。
    private Integer status;
    // 最后登录时间。
    private LocalDateTime lastLoginAt;
    // 是否必须改密。
    private Boolean mustChangePassword;
    // 执行 from。

    public static UserResp from(SysUser u, String deptName) {

        UserResp r = new UserResp();
        r.setId(u.getId());
        r.setUsername(u.getUsername());
        r.setRealName(u.getRealName());
        r.setRole(u.getRole());
        r.setDeptId(u.getDeptId());
        r.setDeptLead(u.getDeptLead());
        r.setEmail(u.getEmail());
        r.setPhone(u.getPhone());
        r.setStatus(u.getStatus());
        r.setLastLoginAt(u.getLastLoginAt());
        r.setMustChangePassword(u.getMustChangePassword());
        r.setDeptName(deptName);
        return r;
    }
}
