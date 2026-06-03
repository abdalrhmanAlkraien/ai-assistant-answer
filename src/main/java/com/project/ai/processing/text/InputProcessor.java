package com.project.ai.processing.text;

import com.project.ai.agents.Language;
import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.InputType;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.util.LanguageDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 10:46 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class InputProcessor {

    public MultimodalRequest process(final String userId, final ChatRequest chatRequest) {

        InputType inputType = resolveInputType(chatRequest.getQuestion(), chatRequest.getImageBase64());
        log.info("[InputProcessor] inputType={}", inputType);

        Language language = resolveLanguage(inputType, chatRequest.getQuestion());
        log.info("[InputProcessor] language={}", language);

        MultimodalRequest request = MultimodalRequest.builder()
                .userId(userId)
                .textQuestion(chatRequest.getQuestion())
                .imageBase64(chatRequest.getImageBase64())
                .imageMediaType(chatRequest.getImageMediaType())
                .inputType(inputType)
                .detectedLanguage(language)
                .build();

        String normalizedText = normalize(request);
        request.setNormalizedText(normalizedText);

        log.info("[InputProcessor] normalizedText='{}'", normalizedText);
        return request;
    }

    private InputType resolveInputType(String text, String imageBase64) {
        boolean hasText  = text != null && !text.isBlank();
        boolean hasImage = imageBase64 != null && !imageBase64.isBlank();

        if (hasImage && hasText)  return InputType.IMAGE_WITH_TEXT;
        if (hasImage)             return InputType.IMAGE;
        return InputType.TEXT;
    }

    private Language resolveLanguage(InputType inputType, String text) {
        return switch (inputType) {
            case TEXT, IMAGE_WITH_TEXT -> LanguageDetector.detect(text);
            case IMAGE -> Language.UNKNOWN;   // detected after image analysis
            default    -> Language.ENGLISH;
        };
    }

    private String normalize(MultimodalRequest request) {
        return switch (request.getInputType()) {
            case TEXT            -> request.getTextQuestion();
            case IMAGE_WITH_TEXT -> request.getTextQuestion(); // image handled by ImageAgent
            case IMAGE           -> null; // ImageAgent will extract text from image
            default              -> request.getTextQuestion();
        };
    }
}
