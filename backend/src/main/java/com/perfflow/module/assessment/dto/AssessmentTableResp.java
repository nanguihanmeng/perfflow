package com.perfflow.module.assessment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 主表 + 行 + 流程 完整响应。
 */
@Data
public class AssessmentTableResp implements Serializable {

    private Long id;
    private Long periodId;
    private Long userId;
    private Long deptId;
    /** 岗位（被考核人填写） */
    private String position;
    private String state;
    private BigDecimal selfTotalScore;
    private BigDecimal leaderScore;
    private BigDecimal finalScore;
    private String grade;
    private Integer suspendExtendedDays;
    private LocalDateTime submittedAt;
    private LocalDateTime pushedAt;
    private LocalDateTime deptApprovedAt;
    private LocalDateTime leadFinishedAt;

    /** 周期名（可选） */
    private String periodName;
    /** 真实姓名（HR/LEAD/部门领导可见） */
    private String realName;
    /** 部门名 */
    private String deptName;
    /** 部门负责人姓名 */
    private String deptLeadName;

    private List<RowResp> rows;
    private List<FlowLogResp> logs;
}
