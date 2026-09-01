package com.perfflow.module.assessment.dto;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
// 行响应。脱敏后某些字段为 null。
@Data
public class RowResp implements Serializable {

    // ID。
    private Long id;
    // 考核主表ID。
    private Long tableId;
    // 指标类别。
    private String category; // PLAN/OPEN/BONUS
    // 序号
    private Integer seq;     // 1-10
    // 指标名称
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
    private BigDecimal selfScore;   // masked -> null
    // 领导评分
    private BigDecimal leaderScore; // masked -> null
    // 是否冻结
    private Boolean frozen;
    private Boolean masked;
}
