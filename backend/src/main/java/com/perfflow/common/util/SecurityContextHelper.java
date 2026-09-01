package com.perfflow.common.util;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.security.DataScopeContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Set;
// 当前线程上下文工具：从 {@link DataScopeContext} 和 SecurityContext 中读取当前用户。

public final class SecurityContextHelper {

    private SecurityContextHelper() {}

    public static long requireUserId() {

        // 取当前用户上下文
        Long uid = DataScopeContext.currentUserId();
        if (uid != null) return uid;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 判空处理
        if (auth == null || auth.getDetails() == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        try {

            return Long.parseLong(String.valueOf(auth.getName()));

        } catch (Exception e) {

            // 校验失败抛异常
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    public static Long currentUserIdOrNull() {

        // 取当前用户上下文
        return DataScopeContext.currentUserId();
    }

    public static String currentRole() {

        // 取当前用户上下文
        Set<String> roles = DataScopeContext.currentRoles();
        if (roles == null || roles.isEmpty()) return null;
        return roles.iterator().next();
    }
}
