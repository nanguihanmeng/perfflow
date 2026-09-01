package com.perfflow.module.assessment.dto;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
@Data
public class FlowLogResp implements Serializable {
    // ID。
    private Long id;
    // 来源状态。
    private String fromState;
    // 目标状态。
    private String toState;
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
