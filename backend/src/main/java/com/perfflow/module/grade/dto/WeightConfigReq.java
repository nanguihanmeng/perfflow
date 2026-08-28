package com.perfflow.module.grade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 权重配置请求。
 */
@Data
public class WeightConfigReq implements Serializable {

    @NotBlank
    private String staffLevel;

    @NotNull
    private BigDecimal deptWeight;

    @NotNull
    private BigDecimal personalWeight;

    private Long periodId;
}
