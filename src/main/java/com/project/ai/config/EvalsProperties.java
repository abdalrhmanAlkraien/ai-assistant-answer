package com.project.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 21/06/2026
 * @Time: 7:20 AM
 */
@ConfigurationProperties(prefix = "app.evals")
@Getter
@Setter
@Component
public class EvalsProperties {

    private boolean enabled = false;
    private String evelsUrl = "http://localhost:8001";
    private Long tokenBudget = 0L;
    private String internalSecret = "";
}
