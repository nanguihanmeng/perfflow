package com.perfflow.module.monitor.service;

import com.perfflow.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 催办服务：定向催办指定人员（写站内通知）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final NotificationService notificationService;

    /**
     * 定向催办。
     *
     * @param targetUserIds 被催办人ID列表
     * @param content       催办内容
     */
    @Transactional(rollbackFor = Exception.class)
    public void remind(List<Long> targetUserIds, String content) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return;
        }
        for (Long userId : targetUserIds) {
            notificationService.send(userId, "考核催办提醒", content, "REMIND");
        }
        log.info("定向催办完成: 人数={}, content={}", targetUserIds.size(), content);
    }
}
