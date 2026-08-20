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
    private Long id;
    private Long tableId;
    /** PLAN / OPEN / BONUS */
    private String category;
    /** 1-10 */
    private Integer seq;
    private String indicatorName;
    private BigDecimal baseScore;
    private String workTarget;
    private String scoreCriteria;
    private BigDecimal completionRate;
    private BigDecimal selfScore;
    private BigDecimal adjustedScore;
    /** DB 可见，前端隐藏 */
    private String adjustRemark;
    private BigDecimal leaderScore;
    private String rowResult;
    private Boolean frozen;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
