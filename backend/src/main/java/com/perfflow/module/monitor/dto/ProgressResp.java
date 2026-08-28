package com.perfflow.module.monitor.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 进度看板响应（汇总卡片 + 部门明细）。
 */
@Data
public class ProgressResp implements Serializable {

    private Long total;
    private Long filled;
    private Long unfilled;
    private Long reviewed;
    private Long overdue;
    private List<DeptProgressResp> deptDetails;
}
