package com.project.ai.loader;

import com.project.ai.model.prompt.BusinessPrompt;
import com.project.ai.repository.BusinessPromptRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 10:54 PM
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class PromptLoader {

    private final BusinessPromptRepository promptRepository;
    @Value("${app.business-strategy}")
    private String businessName;

    private final Map<String, String> promptCache = new ConcurrentHashMap<>();


    @PostConstruct
    public void loadAll() {
        log.info("[PromptLoader] Loading prompts for business='{}'", businessName);

        List<BusinessPrompt> prompts = promptRepository
                .findAllByBusinessNameAndActiveTrue(businessName);

        prompts.forEach(p -> {
            promptCache.put(p.getPromptKey(), p.getPromptTemplate());
            log.info("[PromptLoader] Loaded prompt key='{}'", p.getPromptKey());
        });

        log.info("[PromptLoader] Loaded {} prompts for '{}'",
                promptCache.size(), businessName);
    }

    public String get(String promptKey) {
        String prompt = promptCache.get(promptKey);
        if (prompt == null) {
            throw new IllegalStateException(
                    "Prompt not found for key='" + promptKey +
                            "' business='" + businessName + "'");
        }
        return prompt;
    }

    public void reload(String promptKey) {
        log.info("[PromptLoader] Reloading prompt key='{}'", promptKey);
        promptRepository
                .findByBusinessNameAndPromptKeyAndActiveTrue(businessName, promptKey)
                .ifPresent(p -> {
                    promptCache.put(p.getPromptKey(), p.getPromptTemplate());
                    log.info("[PromptLoader] Reloaded prompt key='{}'", promptKey);
                });
    }

    public void reloadAll() {
        log.info("[PromptLoader] Reloading all prompts for '{}'", businessName);
        promptCache.clear();
        loadAll();
    }

    public void evict(String promptKey) {
        promptCache.remove(promptKey);
        log.info("[PromptLoader] Evicted prompt key='{}' from cache", promptKey);
    }
}
