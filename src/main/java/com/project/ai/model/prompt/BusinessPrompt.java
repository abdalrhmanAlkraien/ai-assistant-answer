package com.project.ai.model.prompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 10:51 PM
 */
@Entity
@Table(name = "business_prompts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String businessName;
    private String promptKey;

    @Column(columnDefinition = "TEXT")
    private String promptTemplate;

    private Integer version;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
