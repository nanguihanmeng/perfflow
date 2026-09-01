package com.perfflow.module.monitor.dto;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
// 进度看板响应（汇总卡片 + 部门明细）。
@Data
public class ProgressResp implements Serializable {

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
    // 部门明细列表。
    private List<DeptProgressResp> deptDetails;
}
