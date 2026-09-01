package com.perfflow.module.audit.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
// 调整审计日志（前台默认不可见，仅运营管理部/管理员可见）。
@Data
@TableName("adjust_log")
public class AdjustLog implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    private String targetType;
    // target Id。
    private Long targetId;
    // field Name。
    private String fieldName;
    // before Value。
    private String beforeValue;
    // after Value。
    private String afterValue;
    // 经办人ID。
    private Long operatorId;
    // 调整原因。
    private String adjustReason;
    private Boolean isVisible;

    private LocalDateTime createdAt;
}
