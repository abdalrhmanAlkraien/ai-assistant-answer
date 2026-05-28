package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 28/05/2026
 * @Time: 11:54 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestsByTypeDto {
    private String type;
    private Long count;
}
