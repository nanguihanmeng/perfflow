package com.perfflow.module.assessment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 行更新请求。
 *
 * <p>按角色分流：
 * <ul>
 *   <li>EMP：completionRate（0-100），自评得分由后端自动算</li>
 *   <li>DEPT_LEAD：selfScore（0~该行指标分数），直接覆盖自评得分</li>
 * </ul>
 */
@Data
public class RowReq implements Serializable {

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal completionRate;

    @DecimalMin("0")
    private BigDecimal selfScore;

    @Size(max = 50)
    private String position;
}
