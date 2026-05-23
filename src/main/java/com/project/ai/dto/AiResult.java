package com.project.ai.dto;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 2:04 AM
 */
public record AiResult<T>(
        T result,
        int inputTokens,
        int outputTokens
) { }
