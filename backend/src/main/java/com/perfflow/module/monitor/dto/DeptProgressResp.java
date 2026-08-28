package com.perfflow.module.monitor.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门进度明细（看板行）。
 */
@Data
public class DeptProgressResp implements Serializable {

    private Long deptId;
    private String deptName;
    private Long total;
    private Long filled;
    private Long unfilled;
    private Long reviewed;
    private Long overdue;
    /** 完成率（0-100） */
    private Double completionRate;
}
