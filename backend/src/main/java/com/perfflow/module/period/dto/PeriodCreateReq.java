package com.perfflow.module.period.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class PeriodCreateReq implements Serializable {

    @NotBlank
    private String name;
    /** 周期类型：{@link com.perfflow.module.period.enums.PeriodType}，年度类型时 quarter 必须为 0 */
    @NotBlank
    private String periodType;

    @NotNull
    @Min(2000) @Max(2100)
    private Integer year;
    /** 季度：年度类型为 0，季度类型 1-4 */
    @NotNull
    @Min(0) @Max(4)
    private Integer quarter;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate suspendEndDate;

    @NotNull
    private LocalDate deptReviewEndDate;

    @NotNull
    private LocalDate leadScoreEndDate;

    private Boolean autoPushOnExpire;
}
