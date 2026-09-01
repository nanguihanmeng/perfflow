package com.perfflow.module.assessment.entity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
@Data
@TableName("assessment_flow_log")
public class AssessmentFlowLog implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    // 考核主表ID。
    private Long tableId;
    // 来源状态。
    private String fromState;
    // 目标状态。
    private String toState;
    // 动作。
    private String action;
    // 经办人ID。
    private Long operatorId;
    // 经办人角色。
    private String operatorRole;
    // 意见。
    private String comment;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
}
