package com.perfflow.module.period.entity;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@TableName("assessment_period")
public class AssessmentPeriod implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    // 名称。
    private String name;
    private String periodType;
    // 年份。
    private Integer year;
    // 季度。
    private Integer quarter;
    // 开始日期。
    private LocalDate startDate;
    // 自评截止日期。
    private LocalDate suspendEndDate;
    // 部门审核截止日期。
    private LocalDate deptReviewEndDate;
    // 领导评分截止日期。
    private LocalDate leadScoreEndDate;
    // 到期是否自动推送。
    private Boolean autoPushOnExpire;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updatedAt;
}
