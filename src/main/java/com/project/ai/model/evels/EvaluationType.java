package com.project.ai.model.evels;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:21 AM
 */
@Entity
@Table(name = "evaluation_type", schema = "evals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @Column(name = "lang", nullable = false)
    private String lang;

    @Column(name = "search_type", nullable = false)
    private String searchType;

    @Column(name = "result_type", nullable = false)
    private String resultType;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "pass_rate", precision = 5, scale = 4)
    private BigDecimal passRate;

    @Column(name = "passed_cases")
    private Integer passedCases;

    @Column(name = "total_cases")
    private Integer totalCases;

    // ── Ragas / list scores ───────────────────────────────────────────────────

    @Column(name = "correct_type_rate", precision = 5, scale = 4)
    private BigDecimal correctTypeRate;

    @Column(name = "right_products_returned", precision = 5, scale = 4)
    private BigDecimal rightProductsReturned;

    @Column(name = "no_missing_products", precision = 5, scale = 4)
    private BigDecimal noMissingProducts;

    @Column(name = "no_hallucination", precision = 5, scale = 4)
    private BigDecimal noHallucination;

    // ── LLM / clarification scores ────────────────────────────────────────────

    @Column(name = "clarification_pass_rate", precision = 5, scale = 4)
    private BigDecimal clarificationPassRate;

    @Column(name = "safe_response_rate", precision = 5, scale = 4)
    private BigDecimal safeResponseRate;

    // ── Extra scores (future metrics) ─────────────────────────────────────────

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_scores", columnDefinition = "jsonb")
    private Map<String, Object> extraScores;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "evaluationType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Context> contexts = new ArrayList<>();
}
