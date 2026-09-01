package com.perfflow.module.system.entity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
@Data
@TableName("sys_department")
public class SysDepartment implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    // 名称。
    private String name;
    // 父部门ID。
    private Long parentId;
    // leader User Id。
    private Long leaderUserId;
    // 排序号。
    private Integer sort;
    // 备注。
    private String remark;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updatedAt;
}
