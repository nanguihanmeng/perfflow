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
    // ID。
    private Long id;
    // 登录名。
    private String username;
    // 密码。
    private String password;
    // 真实姓名。
    private String realName;
    private String role;
    // 部门ID。
    private Long deptId;
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
    private Integer tokenVersion;
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)

    private LocalDateTime createdAt;
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)

    private LocalDateTime updatedAt;
}
