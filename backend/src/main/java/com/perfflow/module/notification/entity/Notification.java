package com.perfflow.module.notification.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
// 站内通知。
@Data
@TableName("notification")
public class Notification implements Serializable {

    @TableId(type = IdType.AUTO)
    // ID。
    private Long id;
    // 目标用户ID。
    private Long targetUserId;
    // 标题。
    private String title;
    // 内容。
    private String content;
    private String type;
    // read Flag。
    private Boolean readFlag;

    private LocalDateTime createdAt;
}
