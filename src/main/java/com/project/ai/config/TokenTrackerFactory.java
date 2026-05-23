package com.project.ai.config;

import com.project.ai.dto.TokenTracker;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 1:37 AM
 */
@Component
public class TokenTrackerFactory {

    public TokenTracker create(String userId, String modelName, String userMessage) {
        return new TokenTracker(
                UUID.randomUUID().toString(),
                userId,
                modelName,
                userMessage
        );
    }

    public TokenTracker create(String modelName, String userMessage) {
        return create(null, modelName, userMessage);
    }
}
