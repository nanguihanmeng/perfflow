package com.perfflow.module.deptassessment.dto;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
// 部门考核响应（含 KPI 行）。
@Data
public class DeptAssessmentResp implements Serializable {

    // ID。
    private Long id;
    // 周期ID。
    private Long periodId;
    // 部门ID。
    private Long deptId;
    // 部门名。
    private String deptName;
    // 经营业绩得分。
    private BigDecimal kpiScore;
    // 运营指标得分。
    private BigDecimal operationScore;
    // 重点工作得分。
    private BigDecimal keyWorkScore;
    // 加减分。
    private BigDecimal bonusScore;
    // 总分。
    private BigDecimal totalScore;
    // 部门等级。
    private String deptGrade;
    // 状态。
    private Integer status;

    private LocalDateTime submittedAt;
    // 复核时间。
    private LocalDateTime reviewedAt;
    // 审批时间。
    private LocalDateTime approvedAt;
    // 版本号。
    private Integer version;
    // 调整原因。
    private String adjustReason;
    // 明细行列表。
    private List<DeptKpiRowResp> rows;
}
