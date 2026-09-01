package com.perfflow.module.grade.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
// 等级配额配置请求。
@Data
public class GradeQuotaReq implements Serializable {

    @NotBlank
    // 部门等级。
    private String deptGrade;
    @NotBlank
    // staff Level。
    private String staffLevel;
    @NotNull
    // grade Ratio。
    private BigDecimal gradeARatio;
    @NotNull
    // grade Ratio。
    private BigDecimal gradeBRatio;
    @NotNull
    // grade Ratio。
    private BigDecimal gradeCRatio;
    @NotNull
    // grade Ratio。
    private BigDecimal gradeDRatio;
    // is Default。
    private Boolean isDefault;
}
