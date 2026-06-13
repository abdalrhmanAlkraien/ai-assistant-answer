package com.project.ai.dto.prompt;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 13/06/2026
 * @Time: 3:58 PM
 */
@Data
public class PromptUpdateRequest {

    @NotBlank(message = "promptTemplate is required")
    private String promptTemplate;

    private String description;   // what changed
    private String updatedBy;     // who changed
    private String changeReason;  // why changed
}
