package com.project.ai.dto.lookup;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:20 AM
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SystemLookupRequest {

    @NotBlank private String type;
    @NotBlank private String code;
    @NotBlank private String label;
    private Boolean active = true;
    private Integer sortOrder;
}
