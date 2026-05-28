package com.project.ai.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 1:27 AM
 */
@Entity
@Table(
        name = "token_request_summary",
        indexes = {
                @Index(name = "idx_token_request_user_id",    columnList = "user_id"),
                @Index(name = "idx_token_request_created_at", columnList = "created_at"),
                @Index(name = "idx_token_request_model_name", columnList = "model_name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequestSummary {

    @Id
    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "user_message", columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "total_input_tokens", nullable = false)
    private Integer totalInputTokens;

    @Column(name = "total_output_tokens", nullable = false)
    private Integer totalOutputTokens;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;

    @Column(name = "total_calls", nullable = false)
    private Integer totalCalls;

    @Column(name = "total_duration_ms", nullable = false)
    private Long totalDurationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "requestSummary", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<TokenCallRecord> callRecords = new ArrayList<>();

    @Column(name = "request_type", length = 50)
    private String requestType;
}
