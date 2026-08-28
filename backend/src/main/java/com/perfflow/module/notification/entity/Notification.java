package com.perfflow.module.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内通知。
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long targetUserId;
    private String title;
    private String content;
    /** {@link com.perfflow.common.enums.NotificationType} */
    private String type;
    private Boolean readFlag;
    private LocalDateTime createdAt;
}
