package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 08/05/2026
 * @Time: 12:31 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {

    private String imageBase64;
    private String imageMediaType;
    private String audioBase64;
    private String audioMediaType;  // "audio/wav", "audio/webm"
    private String question;
}
