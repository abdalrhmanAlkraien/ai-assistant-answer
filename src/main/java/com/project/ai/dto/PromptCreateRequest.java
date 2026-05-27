package com.project.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 28/05/2026
 * @Time: 1:41 AM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptCreateRequest {

    @NotBlank(message = "promptKey is required")
    private String promptKey;

    @NotBlank(message = "promptTemplate is required")
    private String promptTemplate;

    private boolean isActive = true;
}
