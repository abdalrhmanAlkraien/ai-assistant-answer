package com.project.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/05/2026
 * @Time: 10:37 PM
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI chat endpoints")
public class ChatController {

    private final ChatService chatService;


    @Operation(summary = "Send a message", description = "Send a question to the AI assistant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = MultimodalResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/chat/{id}")
    public ResponseEntity<MultimodalResponse> chat(
            @PathVariable("id") final String userId,
            final @RequestBody ChatRequest chatRequest) throws JsonProcessingException {

        return ResponseEntity.ok(chatService.chat(userId, chatRequest));
    }
}
