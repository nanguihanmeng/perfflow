package com.perfflow.module.deptassessment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门考核响应（含 KPI 行）。
 */
@Data
public class DeptAssessmentResp implements Serializable {

    private Long id;
    private Long periodId;
    private Long deptId;
    private String deptName;
    private BigDecimal kpiScore;
    private BigDecimal operationScore;
    private BigDecimal keyWorkScore;
    private BigDecimal bonusScore;
    private BigDecimal totalScore;
    private String deptGrade;
    private Integer status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private Integer version;
    private String adjustReason;
    private List<DeptKpiRowResp> rows;
}
