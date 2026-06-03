package com.project.ai.service;

import com.project.ai.config.TokenTrackerFactory;
import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.dto.TokenTracker;
import com.project.ai.processing.text.InputProcessor;
import com.project.ai.processing.voice.VoiceProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/05/2026
 * @Time: 10:39 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ChatService {

    private final InputProcessor inputProcessor;
    private final TokenTrackerFactory trackerFactory;
    private final PlannerService plannerService;
    private final VoiceProcessor voiceProcessor;

    public MultimodalResponse chat(final String userId, final ChatRequest chatRequest) {

        log.info("[ChatService] START — userId ={}, question={}", userId, chatRequest.getQuestion());

        // ── Voice pre-processing ──────────────────────────────────────────────
        if (chatRequest.getAudioBase64() != null && !chatRequest.getAudioBase64().isBlank()) {
            log.info("[ChatService] Voice input detected — transcribing audio userId={}", userId);

            String transcribed = voiceProcessor.transcribe(
                    chatRequest.getAudioBase64(),
                    chatRequest.getAudioMediaType()
            );

            log.info("[ChatService] Transcription complete — userId={} text='{}'", userId, transcribed);
            chatRequest.setQuestion(transcribed);
        }

        TokenTracker tracker = trackerFactory.create(
                userId,
                "chat-service",
                chatRequest.getQuestion()
        );

        MultimodalRequest multimodalRequest = inputProcessor.process(userId, chatRequest);
        multimodalRequest.setTokenTracker(tracker);
        return plannerService.plan(multimodalRequest);
    }
}
