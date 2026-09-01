package com.perfflow.module.deptassessment.dto;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
// 部门考核填报选项（绩效专员先选择 HR 已开启的考核，再进入填报）。
@Data
public class DeptAssessmentOptionResp implements Serializable {

    private Long assessmentId;
    private Long periodId;
    private String periodName;
    private String periodType;
    private String periodTypeLabel;
    // 年份。
    private Integer year;
    // 季度。
    private Integer quarter;
    // 部门ID。
    private Long deptId;
    // 部门名。
    private String deptName;
    private Integer status;

    private LocalDateTime submittedAt;
}
