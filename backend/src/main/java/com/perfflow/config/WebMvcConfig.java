package com.perfflow.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：挂载管理员业务接口拦截器。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminBusinessGuardInterceptor adminGuard;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(adminGuard)
                .addPathPatterns(
                        "/assessment-tables",
                        "/assessment-tables/**",
                        "/periods",
                        "/periods/**",
                        "/home/**"
                );
    }
}
