package com.perfflow.module.notification.controller;
import com.perfflow.common.api.Result;
import com.perfflow.module.notification.entity.Notification;
import com.perfflow.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
// 站内通知接口。
@Tag(name = "站内通知")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    @GetMapping
    @Operation(summary = "我的通知列表")

    // 查询列表
    public Result<List<Notification>> list() {

        // 返回成功响应
        return Result.ok(notificationService.listMine());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数量")
    // 执行 unreadCount。

    // 业务处理
    public Result<Map<String, Long>> unreadCount() {

        // 返回成功响应
        return Result.ok(Map.of("count", notificationService.unreadCount()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记已读")
    // 执行 markRead。

    // 业务处理
    public Result<Void> markRead(@PathVariable Long id) {

        // 调用业务服务
        notificationService.markRead(id);
        // 返回成功响应
        return Result.ok();
    }
}
