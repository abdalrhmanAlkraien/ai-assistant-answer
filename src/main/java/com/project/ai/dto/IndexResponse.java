package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 9:20 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IndexResponse {

    private int indexed;
    private int failed;
    private String status;
    private Long durationMs;
    private int skipped;
}
