package com.perfflow.module.deptassessment.dto;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
// 部门考核流程日志响应。
@Data
public class DeptFlowLogResp implements Serializable {

    // ID。
    private Long id;
    // 部门考核主表ID。
    private Long assessmentId;
    // 来源状态编码。
    private Integer fromStatus;
    // 目标状态编码。
    private Integer toStatus;
    // 动作。
    private String action;
    // 经办人ID。
    private Long operatorId;
    // 经办人姓名。
    private String operatorName;
    // 经办人角色。
    private String operatorRole;
    // 意见。
    private String comment;

    private LocalDateTime createdAt;
}
