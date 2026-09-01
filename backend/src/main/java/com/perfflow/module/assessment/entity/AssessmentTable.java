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
    // ID。
    private Long id;
    // 周期ID。
    private Long periodId;
    // 用户ID。
    private Long userId;
    // 部门ID。
    private Long deptId;
    private String position;
    private String state;
    // 自评总分。
    private BigDecimal selfTotalScore;
    // 领导评分。
    private BigDecimal leaderScore;
    // 最终得分。
    private BigDecimal finalScore;
    // 等级。
    private String grade;
    private String deptGrade;
    private Integer quotaGradeA;
    // B级名额。
    private Integer quotaGradeB;
    // C级名额。
    private Integer quotaGradeC;
    // D级名额。
    private Integer quotaGradeD;
    private BigDecimal deptScoreWeight;
    // 个人分权重。
    private BigDecimal personalScoreWeight;
    // 挂起累计延长天数。
    private Integer suspendExtendedDays;

    private LocalDateTime submittedAt;
    // pushed At。
    private LocalDateTime pushedAt;
    // dept Approved At。
    private LocalDateTime deptApprovedAt;
    // lead Finished At。
    private LocalDateTime leadFinishedAt;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updatedAt;
}
