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
@TableName("assessment_row")
public class AssessmentRow implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    // 考核主表ID。
    private Long tableId;
    private String category;
    private Integer seq;
    // 指标名称。
    private String indicatorName;
    // 指标分数。
    private BigDecimal baseScore;
    // 工作目标。
    private String workTarget;
    // 评分标准。
    private String scoreCriteria;
    // 完成率。
    private BigDecimal completionRate;
    // 自评得分。
    private BigDecimal selfScore;
    // adjusted Score。
    private BigDecimal adjustedScore;
    private String adjustRemark;
    // 领导评分。
    private BigDecimal leaderScore;
    // row Result。
    private String rowResult;
    // 是否冻结。
    private Boolean frozen;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updatedAt;
}
