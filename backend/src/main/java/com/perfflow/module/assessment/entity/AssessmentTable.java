package com.perfflow.module.assessment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("assessment_table")
public class AssessmentTable implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long periodId;
    private Long userId;
    private Long deptId;
    /** 岗位（被考核人填写） */
    private String position;
    /** {@link com.perfflow.module.assessment.enums.AssessmentState} name */
    private String state;
    private BigDecimal selfTotalScore;
    private BigDecimal leaderScore;
    private BigDecimal finalScore;
    private String grade;
    private Integer suspendExtendedDays;
    private LocalDateTime submittedAt;
    private LocalDateTime pushedAt;
    private LocalDateTime deptApprovedAt;
    private LocalDateTime leadFinishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
