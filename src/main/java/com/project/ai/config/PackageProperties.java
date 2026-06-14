package com.project.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 13/06/2026
 * @Time: 6:19 PM
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.package")
public class PackageProperties {
    private String active;
}
