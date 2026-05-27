package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 11:59 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserMessageDto {

    private Long id;
    private String role;
    private String content;
    private String searchType;
    private List<String> matchedProducts;
    private LocalDateTime createdAt;
}
