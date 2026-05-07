package com.project.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.ChatResponse;
import com.project.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(final @RequestBody ChatRequest chatRequest) throws JsonProcessingException {

        return ResponseEntity.ok(chatService.chat(chatRequest));
    }
}
