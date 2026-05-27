package com.project.ai.controller;

import com.project.ai.loader.PromptLoader;
import com.project.ai.repository.BusinessPromptRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin — Prompts", description = "Manage and reload AI prompts without restart")
public class PromptAdminController {

    private final BusinessPromptRepository promptRepository;
    private final PromptLoader promptLoader;

    @Operation(
            summary = "Update a prompt",
            description = "Updates the prompt template in DB and reloads it into memory immediately — no restart needed"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prompt updated and reloaded"),
            @ApiResponse(responseCode = "404", description = "Prompt key not found", content = @Content)
    })
    @PutMapping("/{promptKey}")
    public ResponseEntity<String> updatePrompt(
            @Parameter(description = "Prompt key e.g. request_analyzer_english", required = true)
            @PathVariable String promptKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New prompt template text. Use %s for placeholders.",
                    required = true,
                    content = @Content(schema = @Schema(type = "string", example = "You are an e-commerce assistant. Question: %s"))
            )
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

        promptLoader.reload(promptKey);

        return ResponseEntity.ok("Prompt updated and reloaded: " + promptKey);
    }

    @Operation(
            summary = "Reload all prompts",
            description = "Reloads all active prompts from DB into memory cache — use after manual DB edits"
    )
    @ApiResponse(responseCode = "200", description = "All prompts reloaded successfully")
    @PostMapping("/reload-all")
    public ResponseEntity<String> reloadAll() {
        promptLoader.reloadAll();
        return ResponseEntity.ok("All prompts reloaded");
    }
}
