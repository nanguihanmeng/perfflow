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
 * <p>只允许改完成率（0-100）；自评得分由后端自动计算，岗位在信息栏单独维护。
 */
@Data
public class RowReq implements Serializable {

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal completionRate;

    @Size(max = 50)
    private String position;
}
