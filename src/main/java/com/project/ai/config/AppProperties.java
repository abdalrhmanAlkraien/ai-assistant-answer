package com.project.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 11/05/2026
 * @Time: 10:39 PM
 */
@ConfigurationProperties(prefix = "app.settings")
@Component
@Data
public class AppProperties {

    private Memory memory = new Memory();

    @Data
    public static class Memory {
        private int context = 20;
        private int similar = 20;
    }
}
