package com.perfflow.module.deptassessment.entity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
// 部门考核流程日志。
@Data
@TableName("dept_assessment_flow_log")
public class DeptAssessmentFlowLog implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    private Long assessmentId;
    private Integer fromStatus;
    private Integer toStatus;
    private String action;
    private Long operatorId;
    private String operatorRole;
    private String comment;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
}
