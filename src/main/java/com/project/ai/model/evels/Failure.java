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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:21 AM
 */
@Entity
@Table(name = "failure", schema = "evals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Failure {

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

    @Column(name = "metric", nullable = false)
    private String metric;

    @Column(name = "score", precision = 7, scale = 4)
    private BigDecimal score;

    @Column(name = "threshold", precision = 7, scale = 4)
    private BigDecimal threshold;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "human_readable", columnDefinition = "TEXT")
    private String humanReadable;
}
