package com.perfflow.module.period.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class PeriodCreateReq implements Serializable {

    @NotBlank
    private String name;
    @NotNull
    @Min(2000) @Max(2100)
    private Integer year;
    @NotNull
    @Min(1) @Max(4)
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
