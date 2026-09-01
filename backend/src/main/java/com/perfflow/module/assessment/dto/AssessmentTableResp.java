package com.perfflow.module.assessment.dto;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
// 主表 + 行 + 流程 完整响应。
@Data
public class AssessmentTableResp implements Serializable {

    // ID。
    private Long id;
    // 周期ID。
    private Long periodId;
    // 用户ID。
    private Long userId;
    // 部门ID。
    private Long deptId;
    private String position;
    // state。
    private String state;
    // 自评总分。
    private BigDecimal selfTotalScore;
    // 领导评分。
    private BigDecimal leaderScore;
    // 最终得分。
    private BigDecimal finalScore;
    // 等级。
    private String grade;
    // 挂起累计延长天数。
    private Integer suspendExtendedDays;

    private LocalDateTime submittedAt;
    // pushed At。
    private LocalDateTime pushedAt;
    // dept Approved At。
    private LocalDateTime deptApprovedAt;
    // lead Finished At。
    private LocalDateTime leadFinishedAt;
    private String periodName;
    private String realName;
    private String deptName;
    private String deptLeadName;
    // 明细行列表。
    private List<RowResp> rows;
    // logs。
    private List<FlowLogResp> logs;
}
