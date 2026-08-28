package com.perfflow.module.deptassessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 部门考核填报请求（含 KPI 行）。
 */
@Data
public class DeptAssessmentReq implements Serializable {

    @NotNull
    private Long deptId;

    /** KPI 明细行（经营业绩/运营指标/重点工作） */
    private List<DeptKpiRowReq> rows;
}
