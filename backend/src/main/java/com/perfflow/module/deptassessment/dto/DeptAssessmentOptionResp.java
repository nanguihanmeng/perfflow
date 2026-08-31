package com.perfflow.module.deptassessment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门考核填报选项（绩效专员先选择 HR 已开启的考核，再进入填报）。
 */
@Data
public class DeptAssessmentOptionResp implements Serializable {

    /** 部门考核主表ID */
    private Long assessmentId;

    /** 周期ID */
    private Long periodId;

    /** 周期名称 */
    private String periodName;

    /** 周期类型编码 */
    private String periodType;

    /** 周期类型中文名 */
    private String periodTypeLabel;

    private Integer year;

    private Integer quarter;

    private Long deptId;

    private String deptName;

    /** 部门考核状态（见 {@link com.perfflow.module.deptassessment.enums.DeptAssessmentState}） */
    private Integer status;

    private LocalDateTime submittedAt;
}
