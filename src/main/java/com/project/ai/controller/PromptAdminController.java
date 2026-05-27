package com.project.ai.controller;

import com.project.ai.dto.PromptCreateRequest;
import com.project.ai.dto.PromptDetailDto;
import com.project.ai.dto.PromptStatsDto;
import com.project.ai.dto.PromptSummaryDto;
import com.project.ai.service.PromptAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    private final PromptAdminService promptAdminService;

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
            @RequestBody String newPromptTemplate) {

        log.info("[PromptAdminController] Updating prompt key='{}'", promptKey);

        promptAdminService.updatePrompt(promptKey, newPromptTemplate);

        return ResponseEntity.ok("Prompt updated and reloaded: " + promptKey);
    }

    @Operation(
            summary = "Reload all prompts",
            description = "Reloads all active prompts from DB into memory cache — use after manual DB edits"
    )
    @ApiResponse(responseCode = "200", description = "All prompts reloaded successfully")
    @PostMapping("/reload-all")
    public ResponseEntity<String> reloadAll() {
        promptAdminService.reloadAll();
        return ResponseEntity.ok("All prompts reloaded");
    }


    @Operation(
            summary = "Get all prompts",
            description = "Returns paginated list of all prompts — template excluded for performance. Use GET /{id} to get full template."
    )
    @ApiResponse(responseCode = "200", description = "Prompts retrieved")
    @GetMapping
    public ResponseEntity<Page<PromptSummaryDto>> getAllPrompts(
            @PageableDefault(size = 20, sort = "businessName") Pageable pageable) {
        return ResponseEntity.ok(promptAdminService.getAllPrompts(pageable));
    }

    @Operation(
            summary = "Get single prompt with template",
            description = "Returns full prompt details including the prompt template"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prompt found"),
            @ApiResponse(responseCode = "404", description = "Prompt not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<PromptDetailDto> getPromptById(
            @Parameter(description = "Prompt ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(promptAdminService.getPromptById(id));
    }

    @Operation(
            summary = "Toggle prompt status",
            description = "Activate or deactivate a prompt. Activating reloads it into memory cache immediately."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "Prompt not found", content = @Content)
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<PromptSummaryDto> toggleStatus(
            @Parameter(description = "Prompt ID", required = true) @PathVariable Long id,
            @Parameter(description = "true to activate, false to deactivate", required = true)
            @RequestParam boolean active) {
        return ResponseEntity.ok(promptAdminService.toggleStatus(id, active));
    }

    @Operation(summary = "Delete a single prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Prompt deleted"),
            @ApiResponse(responseCode = "404", description = "Prompt not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(
            @Parameter(description = "Prompt ID", required = true) @PathVariable Long id) {
        promptAdminService.deletePrompt(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Batch delete prompts",
            description = "Delete multiple prompts by ID list. Skips IDs that don't exist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Prompts deleted"),
            @ApiResponse(responseCode = "400", description = "None of the provided IDs exist", content = @Content)
    })
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deletePrompts(@RequestBody List<Long> ids) {
        promptAdminService.deletePrompts(ids);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create a new prompt",
            description = "Creates a new prompt and loads it into memory cache immediately if active"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prompt created"),
            @ApiResponse(responseCode = "400", description = "Prompt key already exists or invalid request", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PromptDetailDto> createPrompt(
            @RequestBody @Valid PromptCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promptAdminService.createPrompt(request));
    }

    @Operation(
            summary = "Get prompt stats",
            description = "Returns total, active, and inactive prompt counts for the current business"
    )
    @ApiResponse(responseCode = "200", description = "Stats retrieved")
    @GetMapping("/stats")
    public ResponseEntity<PromptStatsDto> getStats() {
        return ResponseEntity.ok(promptAdminService.getStats());
    }
}
