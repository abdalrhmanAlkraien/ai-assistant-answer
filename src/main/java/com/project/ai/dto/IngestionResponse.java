package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 06/05/2026
 * @Time: 7:48 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IngestionResponse {

    private String fileName;
    private Integer size;
    private int totalTokensUsed;
    private String status;
    private String collectionName;
    private LocalDateTime ingestedAt;
}
