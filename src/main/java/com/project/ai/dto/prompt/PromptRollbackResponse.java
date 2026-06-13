package com.project.ai.dto.prompt;

import lombok.Builder;
import lombok.Data;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 13/06/2026
 * @Time: 3:58 PM
 */
@Data
@Builder
public class PromptRollbackResponse {
    private String  promptKey;
    private Integer rolledBackFromVersion;
    private Integer rolledBackToVersion;
    private String  message;
}
