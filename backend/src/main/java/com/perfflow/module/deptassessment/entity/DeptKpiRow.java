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
// 部门 KPI 明细行（经营业绩 / 运营指标 / 重点工作）。
@Data
@TableName("dept_kpi_row")
public class DeptKpiRow implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    // 部门考核主表ID。
    private Long deptAssessmentId;
    private String rowType;
    // 序号。
    private Integer seqNo;
    // 指标名称。
    private String indicatorName;
    // 目标值。
    private String targetValue;
    // 实际完成值。
    private String actualValue;
    // 评分标准。
    private String scoringStandard;
    // 得分。
    private BigDecimal score;
    // 权重。
    private BigDecimal weight;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updatedAt;
}
