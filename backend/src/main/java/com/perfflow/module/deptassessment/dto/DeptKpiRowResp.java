package com.perfflow.module.deptassessment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部门 KPI 行响应。
 */
@Data
public class DeptKpiRowResp implements Serializable {

    private Long id;
    private Long deptAssessmentId;
    private String rowType;
    private Integer seqNo;
    private String indicatorName;
    private String targetValue;
    private String actualValue;
    private String scoringStandard;
    private BigDecimal score;
    private BigDecimal weight;
}
