package com.perfflow.module.deptassessment.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
// 部门考核填报请求（含 KPI 行）。
@Data
public class DeptAssessmentReq implements Serializable {

    @NotNull
    // 部门ID。
    private Long deptId;
    private List<DeptKpiRowReq> rows;
}
