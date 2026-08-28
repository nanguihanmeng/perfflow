package com.perfflow.module.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.perfflow.module.notification.entity.Notification;
import com.perfflow.module.notification.mapper.NotificationMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 站内通知服务：发送 / 我的通知列表 / 标记已读。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 发送站内通知。
     *
     * @param targetUserId 接收人ID
     * @param title        标题
     * @param content      内容
     * @param type         通知类型
     */
    @Transactional(rollbackFor = Exception.class)
    public void send(Long targetUserId, String title, String content, String type) {
        if (targetUserId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setTargetUserId(targetUserId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type == null ? "SYSTEM" : type);
        notification.setReadFlag(false);
        notificationMapper.insert(notification);
        log.info("发送站内通知: targetUserId={}, type={}, title={}", targetUserId, type, title);
    }

    /**
     * 我的通知列表（未读在前）。
     *
     * @return 通知列表
     */
    public List<Notification> listMine() {
        Long userId = DataScopeContext.currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        return notificationMapper.selectList(new QueryWrapper<Notification>()
                .eq("target_user_id", userId)
                .orderByDesc("read_flag")
                .orderByDesc("created_at")
                .last("LIMIT 50"));
    }

    /**
     * 标记通知已读。
     *
     * @param id 通知ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        Long userId = DataScopeContext.currentUserId();
        if (userId == null) {
            return;
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getTargetUserId, userId)
                .set(Notification::getReadFlag, true));
    }

    /**
     * 未读通知数量。
     *
     * @return 未读数量
     */
    public long unreadCount() {
        Long userId = DataScopeContext.currentUserId();
        if (userId == null) {
            return 0L;
        }
        Long count = notificationMapper.selectCount(new QueryWrapper<Notification>()
                .eq("target_user_id", userId)
                .eq("read_flag", 0));
        return count == null ? 0L : count;
    }
}
