package com.perfflow.module.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调整审计日志（前台默认不可见，仅运营管理部/管理员可见）。
 */
@Data
@TableName("adjust_log")
public class AdjustLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** DEPT / PERSONAL */
    private String targetType;
    private Long targetId;
    private String fieldName;
    private String beforeValue;
    private String afterValue;
    private Long operatorId;
    private String adjustReason;
    /** 是否对前台可见（默认 0 不可见） */
    private Boolean isVisible;
    private LocalDateTime createdAt;
}
