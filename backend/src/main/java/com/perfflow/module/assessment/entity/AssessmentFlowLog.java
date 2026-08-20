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
    private Long id;
    private Long tableId;
    private String fromState;
    private String toState;
    private String action;
    private Long operatorId;
    private String operatorRole;
    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
