package com.project.ai.controller;

import com.project.ai.loader.PromptLoader;
import com.project.ai.repository.BusinessPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 11:11 PM
 */
@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
@Log4j2
public class PromptAdminController {

    private final BusinessPromptRepository promptRepository;
    private final PromptLoader promptLoader;

    @PutMapping("/{promptKey}")
    public ResponseEntity<String> updatePrompt(
            @PathVariable String promptKey,
            @RequestBody String newPromptTemplate,
            @Value("${nexai.business-strategy}") String businessName) {

        log.info("[PromptAdminController] Updating prompt key='{}'", promptKey);

        promptRepository.findByBusinessNameAndPromptKeyAndIsActiveTrue(
                        businessName, promptKey)
                .ifPresent(p -> {
                    p.setPromptTemplate(newPromptTemplate);
                    p.setUpdatedAt(LocalDateTime.now());
                    promptRepository.save(p);
                });

        // reload cache immediately — no restart needed
        promptLoader.reload(promptKey);

        return ResponseEntity.ok("Prompt updated and reloaded: " + promptKey);
    }

    @PostMapping("/reload-all")
    public ResponseEntity<String> reloadAll() {
        promptLoader.reloadAll();
        return ResponseEntity.ok("All prompts reloaded");
    }
}
