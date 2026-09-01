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
    // 名称。
    private String name;
    @NotBlank
    // 周期类型。
    private String periodType;
    @NotNull
    @Min(2000) @Max(2100)
    // 年份。
    private Integer year;
    @NotNull
    @Min(0) @Max(4)
    // 季度。
    private Integer quarter;
    @NotNull
    // 开始日期。
    private LocalDate startDate;
    @NotNull
    // 自评截止日期。
    private LocalDate suspendEndDate;
    @NotNull
    // 部门审核截止日期。
    private LocalDate deptReviewEndDate;
    @NotNull
    // 领导评分截止日期。
    private LocalDate leadScoreEndDate;
    // 到期是否自动推送。
    private Boolean autoPushOnExpire;
}
