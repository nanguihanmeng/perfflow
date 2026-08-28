package com.perfflow.module.grade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 等级配额配置请求。
 */
@Data
public class GradeQuotaReq implements Serializable {

    @NotBlank
    private String deptGrade;

    @NotBlank
    private String staffLevel;

    @NotNull
    private BigDecimal gradeARatio;

    @NotNull
    private BigDecimal gradeBRatio;

    @NotNull
    private BigDecimal gradeCRatio;

    @NotNull
    private BigDecimal gradeDRatio;

    private Boolean isDefault;
}
