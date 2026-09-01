package com.perfflow.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.perfflow.common.api.Result;
import com.perfflow.common.api.ResultCode;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
// 解析 Authorization 头中的 Bearer Token，写入 SecurityContext 与 DataScopeContext。
 // 否则判定为旧设备令牌（账号已在其他设备登录），返回 401。
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final SysUserMapper userMapper;
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)

            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 判空处理
        if (header == null || !header.startsWith(BEARER)) {

            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER.length());

        try {

            // 解析令牌
            Claims c = jwtUtil.parse(token);
            String type = c.get("type", String.class);

            // 相等判断
            if (!"access".equals(type)) {

                writeJson(response, HttpStatus.UNAUTHORIZED,
                        // 返回失败响应
                        Result.fail(ResultCode.TOKEN_INVALID, "非访问令牌"));
                return;
            }

            Long userId = Long.valueOf(c.getSubject());
            // 校验令牌版本号：与库内一致才有效（单账号多设备互踢）
            Integer tokenVersion = c.get("tokenVersion", Integer.class);
            // 查询单条
            SysUser dbUser = userMapper.selectById(userId);

            // 判空处理
            if (dbUser == null) {

                writeJson(response, HttpStatus.UNAUTHORIZED,
                        // 返回失败响应
                        Result.fail(ResultCode.TOKEN_INVALID, "用户不存在"));
                return;
            }

            // 令牌版本管理
            Integer dbVersion = dbUser.getTokenVersion() == null ? 0 : dbUser.getTokenVersion();

            // 判空处理
            if (tokenVersion == null || !tokenVersion.equals(dbVersion)) {

                writeJson(response, HttpStatus.UNAUTHORIZED,
                        // 返回失败响应
                        Result.fail(ResultCode.TOKEN_INVALID, "账号已在其他设备登录，请重新登录"));
                return;
            }

            String username = c.get("username", String.class);
            String role = c.get("role", String.class); // 主角色不带 ROLE_
            Long deptId = c.get("deptId", Long.class);
            Boolean deptLead = c.get("deptLead", Boolean.class);
            Boolean mustChg = c.get("mustChangePwd", Boolean.class);
            Set<String> roleAuths = new HashSet<>();

            // 非空才处理
            if (role != null) {

                roleAuths.add("ROLE_" + role);
            }

            // ---- 写入 SecurityContext ----
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null,
                    roleAuths.stream().map(SimpleGrantedAuthority::new).toList()
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
            // ---- 写入 DataScopeContext ----
            DataScopeContext.set(DataScopeContext.CurrentUser.builder()
                    .userId(userId).username(username).primaryRole(role)
                    .deptId(deptId).deptLead(deptLead).mustChangePassword(mustChg)
                    .roles(roleAuths).build());
            chain.doFilter(request, response);

        } catch (JwtException e) {

            log.debug("jwt invalid: {}", e.getMessage());
            writeJson(response, HttpStatus.UNAUTHORIZED,
                    // 返回失败响应
                    Result.fail(ResultCode.TOKEN_INVALID, "凭证无效或已过期"));

        } finally {
            // 出 filter 时清除上下文，防止线程复用泄漏
            SecurityContextHolder.clearContext();
            DataScopeContext.clear();
        }
    }

    // 写入 Json
    private void writeJson(HttpServletResponse resp, HttpStatus status, Result<?> body) throws IOException {

        // 设置状态
        resp.setStatus(status.value());
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
