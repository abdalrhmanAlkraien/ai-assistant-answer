package com.project.ai.dto;

import com.project.ai.agents.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 10:05 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MultimodalRequest {

    private Long userId;
    private String textQuestion;        // text input
    private String audioBase64;         // voice input (base64)
    private String imageBase64;         // image input (base64)
    private String imageMediaType;      // image/jpeg, image/png
    private Language detectedLanguage;    // set by LanguageDetector
    private String normalizedText;      // set by InputProcessor
    private InputType inputType;        // TEXT, VOICE, IMAGE, IMAGE_WITH_TEXT
    private TokenTracker tokenTracker;
}
