package com.project.ai.model.prompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 10:51 PM
 */
@Entity
@Table(
        name = "business_prompts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_prompt_version",
                        columnNames = {"business_name", "prompt_key", "version"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_prompt_active",
                        columnList = "business_name, prompt_key, is_active"
                )
        }
)@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_name", nullable = false, length = 100)
    private String businessName;

    @Column(name = "prompt_key", nullable = false, length = 100)
    private String promptKey;

    @Column(name = "prompt_template", columnDefinition = "TEXT", nullable = false)
    private String promptTemplate;

    @Column(name = "version", nullable = false)
    @Min(value = 1, message = "Version must be at least 1")
    private Integer version;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;          // what changed in this version

    @Column(name = "updated_by", length = 100)
    private String updatedBy;            // who changed it

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;         // why it was changed

    @Column(name = "eval_score", precision = 5, scale = 4)
    private BigDecimal evalScore;        // best eval score achieved

    @Column(name = "eval_run_at")
    private LocalDateTime evalRunAt;     // when last eval was run

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt; // when it was replaced

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (version == null) version = 1;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
