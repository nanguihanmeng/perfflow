package com.perfflow.security;

import com.perfflow.common.constant.RoleConst;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 数据权限上下文过滤器。
 *
 * <p>在 JWT 过滤器之后执行，将当前登录用户的关键信息封装进 {@link DataScopeContext},
 * 供后续业务层做数据权限过滤；请求结束自动清理 ThreadLocal。
 *
 * <p>匿名访问不装配上下文（白名单路径已由 SecurityConfig 放行，此处再防御一次）。
 *
 * @author PerfFlow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeContextFilter extends OncePerRequestFilter {

    /** 匿名请求标记，由 Spring Security 在未登录场景写入。 */
    private static final String ANONYMOUS_PRINCIPAL = "anonymousUser";

    private final SysUserMapper userMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse resp,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            assembleContext();
            chain.doFilter(req, resp);
        } finally {
            DataScopeContext.clear();
        }
    }

    /**
     * 从当前 SecurityContext 提取用户，转换为 {@link DataScopeContext}。
     * 未认证或用户不存在则不设置（后续业务层会被 @PreAuthorize 拦截）。
     */
    private void assembleContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null || ANONYMOUS_PRINCIPAL.equals(principal)) {
            return;
        }
        if (!(principal instanceof SysUser user)) {
            return;
        }
        SysUser fresh = userMapper.selectById(user.getId());
        if (fresh == null) {
            return;
        }
        boolean isDeptLead = authentication.getAuthorities().stream()
                .anyMatch(a -> RoleConst.ROLE_DEPT_LEAD.equals(a.getAuthority()));
        DataScopeContext.set(DataScopeContext.CurrentUser.of(fresh, isDeptLead));
    }
}
