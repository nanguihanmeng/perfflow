package com.perfflow.common.util;

import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.security.DataScopeContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * 当前线程上下文工具：从 {@link DataScopeContext} 和 SecurityContext 中读取当前用户。
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {}

    /** 当前用户 id（long），无则抛 401。 */
    public static long requireUserId() {
        Long uid = DataScopeContext.currentUserId();
        if (uid != null) return uid;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        try {
            return Long.parseLong(String.valueOf(auth.getName()));
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    public static Long currentUserIdOrNull() {
        return DataScopeContext.currentUserId();
    }

    public static String currentRole() {
        Set<String> roles = DataScopeContext.currentRoles();
        if (roles == null || roles.isEmpty()) return null;
        return roles.iterator().next();
    }
}
