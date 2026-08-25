package com.perfflow.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String realName;
    /** EMP/DEPT_LEAD/LEAD/HR/ADMIN */
    private String role;
    private Long deptId;
    private Boolean deptLead;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private Boolean mustChangePassword;
    /** 登录令牌版本号（多设备互踢：新登录自增，旧 token 失效） */
    private Integer tokenVersion;
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
