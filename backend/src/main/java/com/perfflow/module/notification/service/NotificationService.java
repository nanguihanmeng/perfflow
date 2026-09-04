package com.perfflow.module.notification.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.perfflow.module.notification.entity.Notification;
import com.perfflow.module.notification.mapper.NotificationMapper;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
// 站内通知服务：发送 / 我的通知列表 / 标记已读。
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int STATUS_ENABLED = 1;
    private final NotificationMapper notificationMapper;
    private final SysUserMapper userMapper;
    // 发送站内通知。
    @Transactional(rollbackFor = Exception.class)

    // 执行业务处理
    public void send(Long targetUserId, String title, String content, String type) {

        // 判空处理
        if (targetUserId == null) {

            return;
        }

        Notification notification = new Notification();
        notification.setTargetUserId(targetUserId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type == null ? "SYSTEM" : type);
        notification.setReadFlag(false);
        // 写入记录
        notificationMapper.insert(notification);
        log.info("发送站内通知: targetUserId={}, type={}, title={}", targetUserId, type, title);
    }

    // 向指定角色的全部启用账号发送站内通知（同一事务，全量成功或全量回滚）。
    @Transactional(rollbackFor = Exception.class)
    public void sendToRole(String role, String title, String content, String type) {
        List<SysUser> targets = userMapper.selectList(new QueryWrapper<SysUser>()
                .eq("role", role)
                .eq("status", STATUS_ENABLED));
        if (targets.isEmpty()) {
            log.warn("发送站内通知失败，该角色无启用账号: role={}, title={}", role, title);
            return;
        }
        String realType = type == null ? "SYSTEM" : type;
        for (SysUser target : targets) {
            Notification notification = new Notification();
            notification.setTargetUserId(target.getId());
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(realType);
            notification.setReadFlag(false);
            notificationMapper.insert(notification);
        }
        log.info("发送角色站内通知: role={}, 目标数={}, title={}", role, targets.size(), title);
    }

    // 我的通知列表（未读在前）。

    // 查询列表数据
    public List<Notification> listMine() {

        // 取当前用户上下文
        Long userId = DataScopeContext.currentUserId();

        // 判空处理
        if (userId == null) {

            // 空结果返回空集合
            return Collections.emptyList();
        }

        // 查询列表
        return notificationMapper.selectList(new QueryWrapper<Notification>()
                .eq("target_user_id", userId)
                .orderByDesc("read_flag")
                .orderByDesc("created_at")
                .last("LIMIT 50"));
    }

    // 标记通知已读。
    @Transactional(rollbackFor = Exception.class)
    // 执行 markRead。

    // 执行业务处理
    public void markRead(Long id) {

        // 取当前用户上下文
        Long userId = DataScopeContext.currentUserId();

        // 判空处理
        if (userId == null) {

            return;
        }

        // 更新记录
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getTargetUserId, userId)
                .set(Notification::getReadFlag, true));
    }

    // 未读通知数量。

    // 执行业务处理
    public long unreadCount() {

        // 取当前用户上下文
        Long userId = DataScopeContext.currentUserId();

        // 判空处理
        if (userId == null) {

            return 0L;
        }

        // 统计数量
        Long count = notificationMapper.selectCount(new QueryWrapper<Notification>()
                .eq("target_user_id", userId)
                .eq("read_flag", 0));
        return count == null ? 0L : count;
    }
}
