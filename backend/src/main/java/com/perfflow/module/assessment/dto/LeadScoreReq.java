package com.perfflow.module.assessment.dto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
@Data
public class LeadScoreReq implements Serializable {

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    // 领导评分。
    private BigDecimal leaderScore;
    @Size(max = 500)
    // 意见。
    private String comment;
}
