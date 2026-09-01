package com.perfflow.module.grade.entity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
// 权重配置（员工层级 → 部门分权重 + 个人分权重）。
@Data
@TableName("weight_config")
public class WeightConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    // staff Level。
    private String staffLevel;
    // dept Weight。
    private BigDecimal deptWeight;
    // personal Weight。
    private BigDecimal personalWeight;
    // 周期ID。
    private Long periodId;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updatedAt;
}
