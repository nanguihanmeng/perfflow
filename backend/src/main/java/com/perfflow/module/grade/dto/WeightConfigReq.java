package com.perfflow.module.grade.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
// 权重配置请求。
@Data
public class WeightConfigReq implements Serializable {

    @NotBlank
    // staff Level。
    private String staffLevel;
    @NotNull
    // dept Weight。
    private BigDecimal deptWeight;
    @NotNull
    // personal Weight。
    private BigDecimal personalWeight;
    // 周期ID。
    private Long periodId;
}
