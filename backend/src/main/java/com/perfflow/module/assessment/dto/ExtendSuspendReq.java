package com.perfflow.module.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class ExtendSuspendReq implements Serializable {

    @NotNull
    @Min(1)
    @Max(30)
    private Integer days;

    @Size(max = 500)
    private String reason;
}
