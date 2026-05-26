package com.project.ai.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 12:08 AM
 */
@Component
@Log4j2
public class BusinessStrategyLoader {

    private final BusinessStrategy activeStrategy;

    public BusinessStrategyLoader(
            @Value("${app.business-strategy}") String strategyName,
            List<BusinessStrategy> strategies) {

        this.activeStrategy = strategies.stream()
                .filter(s -> s.name().equals(strategyName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No business strategy found for: " + strategyName));

        log.info("[BusinessStrategyLoader] Active strategy: {}", strategyName);
    }

    public BusinessStrategy getActive() {
        return activeStrategy;
    }
}
