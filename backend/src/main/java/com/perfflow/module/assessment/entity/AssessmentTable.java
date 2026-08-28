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
    /** 部门等级（冗余，等级联动写入） */
    private String deptGrade;
    /** 该员工所在部门各等级名额 */
    private Integer quotaGradeA;
    private Integer quotaGradeB;
    private Integer quotaGradeC;
    private Integer quotaGradeD;
    /** 权重快照 */
    private BigDecimal deptScoreWeight;
    private BigDecimal personalScoreWeight;
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
