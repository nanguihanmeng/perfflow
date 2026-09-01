package com.perfflow.module.assessment.dto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
// 行更新请求。
@Data
public class RowReq implements Serializable {

    @DecimalMin("0")
    @DecimalMax("100")
    // 完成率。
    private BigDecimal completionRate;
    @DecimalMin("0")
    // 自评得分。
    private BigDecimal selfScore;
    @Size(max = 50)
    // position。
    private String position;
}
