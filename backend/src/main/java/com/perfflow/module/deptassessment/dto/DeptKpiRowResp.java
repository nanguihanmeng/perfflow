package com.perfflow.module.deptassessment.dto;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
// 部门 KPI 行响应。
@Data
public class DeptKpiRowResp implements Serializable {

    // ID。
    private Long id;
    // 部门考核主表ID。
    private Long deptAssessmentId;
    // 行类型。
    private String rowType;
    // 序号。
    private Integer seqNo;
    // 指标名称。
    private String indicatorName;
    // 目标值。
    private String targetValue;
    // 实际完成值。
    private String actualValue;
    // 评分标准。
    private String scoringStandard;
    // 得分。
    private BigDecimal score;
    // 权重。
    private BigDecimal weight;
}
