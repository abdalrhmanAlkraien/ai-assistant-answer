package com.project.ai.model.evels;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:21 AM
 */
@Entity
@Table(name = "context", schema = "evals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Context {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_type_id", nullable = false)
    private EvaluationType evaluationType;

    @Column(name = "question", nullable = false)
    private String question;

    @Column(name = "language")
    private String language;

    @Column(name = "search_type")
    private String searchType;

    @Column(name = "expected_search_type")
    private String expectedSearchType;

    @Column(name = "returned_search_type")
    private String returnedSearchType;

    @Column(name = "correct_type")
    private Boolean correctType;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "status")
    private String status;

    @Column(name = "has_answer")
    private Boolean hasAnswer;

    @Column(name = "has_contexts")
    private Boolean hasContexts;

    @Column(name = "product_count")
    private Integer productCount;

    @Column(name = "expected_product_count")
    private Integer expectedProductCount;

    // ── LLM judge ─────────────────────────────────────────────────────────────

    @Column(name = "llm_judge")
    private Integer llmJudge;

    // ── Security ──────────────────────────────────────────────────────────────

    @Column(name = "no_error")
    private Boolean noError;

    // ── Pass/fail ─────────────────────────────────────────────────────────────

    @Column(name = "passed")
    private Boolean passed;

    // ── Report fields ─────────────────────────────────────────────────────────

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "ground_truth", columnDefinition = "TEXT")
    private String groundTruth;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contexts", columnDefinition = "jsonb")
    private List<String> contexts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_product_ids", columnDefinition = "jsonb")
    private List<String> matchedProductIds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
