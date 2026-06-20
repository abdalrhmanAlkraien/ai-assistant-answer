package com.project.ai.config;

import com.project.ai.interceptor.PackageAccessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 13/06/2026
 * @Time: 6:24 PM
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PackageAccessInterceptor packageAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(packageAccessInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}
