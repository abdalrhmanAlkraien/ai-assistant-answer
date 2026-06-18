package com.project.ai.dto.evels;

import lombok.Builder;
import lombok.Getter;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:51 AM
 */
@Builder
@Getter
public class ContextPageDto {

    private Long id;
    private Integer index;         // row number (#)
    private String question;
    private String searchType;     // colored badge
    private String expectedSearchType;
    private String returnedSearchType;
    private String language;       // ENGLISH / ARABIC badge
    private Integer latencyMs;     // displayed as "980ms"
    private String status;         // PASS / FAIL badge
    private String error;          // "Type mismatch" or null → "—"
}
