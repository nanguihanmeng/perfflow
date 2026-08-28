package com.perfflow.module.deptassessment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 部门考核主表。
 */
@Data
@TableName("dept_assessment")
public class DeptAssessment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long periodId;
    private Long deptId;
    private BigDecimal kpiScore;
    private BigDecimal operationScore;
    private BigDecimal keyWorkScore;
    private BigDecimal bonusScore;
    private BigDecimal totalScore;
    /** A / B / C / D */
    private String deptGrade;
    /** {@link com.perfflow.module.deptassessment.enums.DeptAssessmentState} code */
    private Integer status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    /** 调整版本号（调整留痕） */
    private Integer version;
    private String adjustReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
