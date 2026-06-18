package com.project.ai.dto.lookup;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:17 AM
 */
@Builder
@Getter
public class SystemLookupDto {
    private Long id;
    private String type;
    private String code;
    private String label;
    private Boolean active;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
