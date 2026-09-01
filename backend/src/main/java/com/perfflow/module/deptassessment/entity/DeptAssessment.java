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
// 部门考核主表。
@Data
@TableName("dept_assessment")
public class DeptAssessment implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    // 周期ID。
    private Long periodId;
    // 部门ID。
    private Long deptId;
    // 经营业绩得分。
    private BigDecimal kpiScore;
    // 运营指标得分。
    private BigDecimal operationScore;
    // 重点工作得分。
    private BigDecimal keyWorkScore;
    // 加减分。
    private BigDecimal bonusScore;
    // 总分。
    private BigDecimal totalScore;
    private String deptGrade;
    private Integer status;

    private LocalDateTime submittedAt;
    // 复核时间。
    private LocalDateTime reviewedAt;
    // 审批时间。
    private LocalDateTime approvedAt;
    private Integer version;
    // 调整原因。
    private String adjustReason;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updatedAt;
}
