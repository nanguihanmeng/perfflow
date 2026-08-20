package com.perfflow.module.system.dto;

import com.perfflow.module.system.entity.SysUser;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserResp implements Serializable {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private Long deptId;
    private String deptName;
    private Boolean deptLead;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private Boolean mustChangePassword;

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
