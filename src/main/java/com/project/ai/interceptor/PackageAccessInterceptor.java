package com.project.ai.interceptor;

import com.project.ai.config.PackageProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 13/06/2026
 * @Time: 6:20 PM
 */
@Data
@Configuration
@Log4j2
public class PackageAccessInterceptor implements HandlerInterceptor {

    private final PackageProperties packageProperties;

    private static final List<String> BASIC_ENDPOINTS = List.of(
            "/api/v1/chat/**",
            "/api/v1/products/**",
            "/api/admin/prompts",
            "/api/admin/prompts/**"
    );

    private static final List<String> GROWTH_ENDPOINTS = List.of(
            "/api/v1/chat/**",
            "/api/v1/products/**",
            "/api/admin/prompts",
            "/api/admin/prompts/**",
            "/api/growth/analytics/**",
            "/api/growth/evals/**",
            "/api/growth/security-log/**",
            "/**"
    );

    private static final List<String> ENTERPRISE_ENDPOINTS = List.of(
            "/**"
    );

    private static final Map<String, List<String>> PACKAGE_ENDPOINTS = Map.of(
            "basic",      BASIC_ENDPOINTS,
            "growth",     GROWTH_ENDPOINTS,
            "enterprise", ENTERPRISE_ENDPOINTS
    );

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String requestPath  = request.getRequestURI();
        String activePackage = packageProperties.getActive();

        List<String> allowed = PACKAGE_ENDPOINTS.getOrDefault(activePackage, List.of());

        boolean permitted = allowed.stream()
                .anyMatch(pattern -> matchesPattern(pattern, requestPath));

        if (!permitted) {
            log.warn("[PackageAccessInterceptor] FORBIDDEN — package='{}' path='{}'",
                    activePackage, requestPath);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                      "error": "Access denied",
                      "message": "This endpoint is not available in your current package: '%s'. Please upgrade to access this feature.",
                      "package": "%s",
                      "path": "%s"
                    }
                    """.formatted(activePackage, activePackage, requestPath));
            return false;
        }

        return true;
    }

    private boolean matchesPattern(String pattern, String path) {
        if ("/**".equals(pattern)) return true;
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return pattern.equals(path);
    }
}
