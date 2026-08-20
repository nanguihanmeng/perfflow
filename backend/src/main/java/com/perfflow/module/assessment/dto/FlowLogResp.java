package com.perfflow.module.assessment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FlowLogResp implements Serializable {
    private Long id;
    private String fromState;
    private String toState;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String comment;
    private LocalDateTime createdAt;
}
