package com.perfflow.module.deptassessment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 部门 KPI 明细行（经营业绩 / 运营指标 / 重点工作）。
 */
@Data
@TableName("dept_kpi_row")
public class DeptKpiRow implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deptAssessmentId;
    /** 经营业绩 / 运营指标 / 重点工作 */
    private String rowType;
    private Integer seqNo;
    private String indicatorName;
    private String targetValue;
    private String actualValue;
    private String scoringStandard;
    private BigDecimal score;
    private BigDecimal weight;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
