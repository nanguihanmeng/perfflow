package com.perfflow.module.deptassessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部门 KPI 行请求。
 */
@Data
public class DeptKpiRowReq implements Serializable {

    @NotNull
    private String rowType;

    @NotNull
    private Integer seqNo;

    private String indicatorName;
    private String targetValue;
    private String actualValue;
    private String scoringStandard;
    private BigDecimal score;
    private BigDecimal weight;
}
