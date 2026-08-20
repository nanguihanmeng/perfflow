package com.perfflow.module.assessment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 行响应。脱敏后某些字段为 null。
 */
@Data
public class RowResp implements Serializable {

    private Long id;
    private Long tableId;
    private String category; // PLAN/OPEN/BONUS
    private Integer seq;     // 1-10
    private String indicatorName;
    private BigDecimal baseScore;
    private String workTarget;
    private String scoreCriteria;
    private BigDecimal completionRate;
    private BigDecimal selfScore;   // masked -> null
    private BigDecimal leaderScore; // masked -> null
    private Boolean frozen;

    /** 是否对该行做了脱敏（用于前端提示） */
    private Boolean masked;
}
