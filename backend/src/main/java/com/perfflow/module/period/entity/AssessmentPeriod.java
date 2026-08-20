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
    private Long id;
    private String name;
    private Integer year;
    private Integer quarter;
    private LocalDate startDate;
    private LocalDate suspendEndDate;
    private LocalDate deptReviewEndDate;
    private LocalDate leadScoreEndDate;
    private Boolean autoPushOnExpire;
    /** 0=未开始 1=进行中 2=已结束 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
