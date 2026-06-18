package com.project.ai.model.evels;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:19 AM
 */
@Entity
@Table(name = "evaluation", schema = "evals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "lang", nullable = false, columnDefinition = "TEXT[]")
    private String[] lang;

    @Column(name = "result_type", nullable = false)
    private String resultType;

    @Column(name = "status", nullable = false)
    private String status = "in_progress";

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "total_evaluated", nullable = false)
    private Integer totalEvaluated = 0;

    @Column(name = "total_skipped", nullable = false)
    private Integer totalSkipped = 0;

    @Column(name = "has_inconclusive", nullable = false)
    private Boolean hasInconclusive = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "report_status")
    private String reportStatus;   // "pending" | "uploaded" | "failed"

    @Column(name = "report_url")
    private String reportUrl;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationType> evaluationTypes = new ArrayList<>();

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Failure> failures = new ArrayList<>();
}
