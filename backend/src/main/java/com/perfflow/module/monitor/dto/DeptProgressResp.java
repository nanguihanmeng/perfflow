package com.perfflow.module.monitor.dto;
import lombok.Data;
import java.io.Serializable;
// 部门进度明细（看板行）。
@Data
public class DeptProgressResp implements Serializable {

    // 部门ID。
    private Long deptId;
    // 部门名。
    private String deptName;
    // 总数。
    private Long total;
    // 已填报数。
    private Long filled;
    // 未填报数。
    private Long unfilled;
    // 已审核数。
    private Long reviewed;
    // 逾期数。
    private Long overdue;
    private Double completionRate;
}
