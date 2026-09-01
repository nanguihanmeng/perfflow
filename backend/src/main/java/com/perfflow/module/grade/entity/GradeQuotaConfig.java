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
// 等级名额配置（部门等级 × 员工层级 → A/B/C/D 比例）。
@Data
@TableName("grade_quota_config")
public class GradeQuotaConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    // 部门等级。
    private String deptGrade;
    // staff Level。
    private String staffLevel;
    // grade Ratio。
    private BigDecimal gradeARatio;
    // grade Ratio。
    private BigDecimal gradeBRatio;
    // grade Ratio。
    private BigDecimal gradeCRatio;
    // grade Ratio。
    private BigDecimal gradeDRatio;
    // is Default。
    private Boolean isDefault;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updatedAt;
}
