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
// 业务接口守卫拦截器。
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBusinessGuardInterceptor implements HandlerInterceptor {

    private static final int HTTP_FORBIDDEN = 403;
    private static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";
    private final ObjectMapper objectMapper;
    @Override
    // 执行 preHandle。
    public boolean preHandle(@NonNull HttpServletRequest req,
                             @NonNull HttpServletResponse resp,
                             @NonNull Object handler) throws IOException {

        // 取当前用户上下文
        CurrentUser current = DataScopeContext.current();

        if (current != null && RoleConst.ROLE_ADMIN.equals(current.getPrimaryRole())) {

            log.warn("admin tried to access business interface: {}", req.getRequestURI());
            // 设置状态
            resp.setStatus(HTTP_FORBIDDEN);
            resp.setContentType(CONTENT_TYPE_JSON_UTF8);
            objectMapper.writeValue(resp.getWriter(),
                    // 返回失败响应
                    Result.fail(ResultCode.ADMIN_NO_BUSINESS_VISIBILITY));
            return false;
        }

        return true;
    }
}
