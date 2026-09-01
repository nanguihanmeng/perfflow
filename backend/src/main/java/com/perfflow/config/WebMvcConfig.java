package com.perfflow.config;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
// Web MVC 配置：挂载管理员业务接口拦截器。
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminBusinessGuardInterceptor adminGuard;
    @Override
    // 新增interceptors。

    public void addInterceptors(@NonNull InterceptorRegistry registry) {

        registry.addInterceptor(adminGuard)
                .addPathPatterns(
                        "/assessment-tables",
                        "/assessment-tables/**",
                        "/periods",
                        "/periods/**",
                        "/home/**",
                        // 企业级升级新增模块（ADMIN 不可访问）
                        "/dept-assessments",
                        "/dept-assessments/**",
                        "/monitor",
                        "/monitor/**",
                        "/notifications",
                        "/notifications/**",
                        "/audit-logs",
                        "/audit-logs/**"
                );
    }
}
