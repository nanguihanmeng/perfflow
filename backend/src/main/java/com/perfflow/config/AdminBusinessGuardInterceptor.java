package com.perfflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.perfflow.common.api.Result;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.security.DataScopeContext;
import com.perfflow.security.DataScopeContext.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 业务接口守卫拦截器。
 *
 * <p>除 /admin/** 之外的业务接口，拒绝 ROLE_ADMIN 访问，实现"管理员不接触业务数据"的物理隔离。
 *
 * @author PerfFlow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBusinessGuardInterceptor implements HandlerInterceptor {

    /** HTTP 403 Forbidden。 */
    private static final int HTTP_FORBIDDEN = 403;

    /** JSON 响应 Content-Type，含 UTF-8 字符集。 */
    private static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest req,
                             @NonNull HttpServletResponse resp,
                             @NonNull Object handler) throws IOException {
        CurrentUser current = DataScopeContext.current();
        if (current != null && RoleConst.ROLE_ADMIN.equals(current.getPrimaryRole())) {
            log.warn("admin tried to access business interface: {}", req.getRequestURI());
            resp.setStatus(HTTP_FORBIDDEN);
            resp.setContentType(CONTENT_TYPE_JSON_UTF8);
            objectMapper.writeValue(resp.getWriter(),
                    Result.fail(ResultCode.ADMIN_NO_BUSINESS_VISIBILITY));
            return false;
        }
        return true;
    }
}
