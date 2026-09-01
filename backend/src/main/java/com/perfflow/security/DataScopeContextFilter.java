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
// 数据权限上下文过滤器。
 // 供后续业务层做数据权限过滤；请求结束自动清理 ThreadLocal。
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeContextFilter extends OncePerRequestFilter {

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

    // 从当前 SecurityContext 提取用户，转换为 {@link DataScopeContext}。
     // 未认证或用户不存在则不设置（后续业务层会被 @PreAuthorize 拦截）。

    private void assembleContext() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 判空处理
        if (authentication == null || !authentication.isAuthenticated()) {

            return;
        }

        Object principal = authentication.getPrincipal();

        // 判空处理
        if (principal == null || ANONYMOUS_PRINCIPAL.equals(principal)) {

            return;
        }

        // 条件分支
        if (!(principal instanceof SysUser user)) {

            return;
        }

        // 查询单条
        SysUser fresh = userMapper.selectById(user.getId());

        // 判空处理
        if (fresh == null) {

            return;
        }

        boolean isDeptLead = authentication.getAuthorities().stream()
                .anyMatch(a -> RoleConst.ROLE_DEPT_LEAD.equals(a.getAuthority()));
        DataScopeContext.set(DataScopeContext.CurrentUser.of(fresh, isDeptLead));
    }
}
