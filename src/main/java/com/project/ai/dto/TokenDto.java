package com.project.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 1:39 AM
 */
public class TokenDto {

    // ── Single call breakdown ─────────────────────────────────────────────────

    public record CallRecordDto(
            String callName,
            int inputTokens,
            int outputTokens,
            int totalTokens,
            long durationMs,
            LocalDateTime calledAt
    ) {}

    // ── Single request detail ─────────────────────────────────────────────────

    public record RequestSummaryDto(
            String requestId,
            String userId,
            String modelName,
            String userMessage,
            int totalInputTokens,
            int totalOutputTokens,
            int totalTokens,
            int totalCalls,
            long totalDurationMs,
            LocalDateTime createdAt,
            List<CallRecordDto> callRecords
    ) {}

    // ── Per-call-name aggregation ─────────────────────────────────────────────

    public record CallAggregateDto(
            String callName,
            long totalInputTokens,
            long totalOutputTokens,
            long grandTotalTokens,
            double avgDurationMs,
            long callCount
    ) {}

    // ── Global summary (all requests) ─────────────────────────────────────────

    public record GlobalSummaryDto(
            long totalRequests,
            long totalInputTokens,
            long totalOutputTokens,
            long grandTotalTokens,
            List<CallAggregateDto> breakdown
    ) {}

    // ── Per-user summary ──────────────────────────────────────────────────────

    public record UserSummaryDto(
            String userId,
            long totalRequests,
            long totalInputTokens,
            long totalOutputTokens,
            long grandTotalTokens,
            List<CallAggregateDto> breakdown
    ) {}
}
