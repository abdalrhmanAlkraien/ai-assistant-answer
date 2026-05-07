package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 08/05/2026
 * @Time: 12:31 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatResponse {

    private String answer;
    List<String> matchProducts;
    private LocalDateTime responseTime;
}
