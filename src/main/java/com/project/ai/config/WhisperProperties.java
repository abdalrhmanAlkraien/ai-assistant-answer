package com.project.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 3:51 PM
 */
@Data
@Component
@ConfigurationProperties(prefix = "openai.whisper")
public class WhisperProperties {

    private String apiKey;
    private String model = "whisper-1";
    private String language = "auto";
}
