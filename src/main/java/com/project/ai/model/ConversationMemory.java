package com.project.ai.model;

import com.project.ai.agents.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * @Date: 11/05/2026
 * @Time: 9:22 PM
 */
@Table(
        name = "conversation_memory",
        indexes = {
                @Index(name = "idx_memory_user_id", columnList = "user_id"),
                @Index(name = "idx_memory_created_at", columnList = "created_at")
        }
)
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageRole role;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "message_vector", columnDefinition = "vector(384)")
    private String messageVector;

    @Column(name = "search_type")
    private String searchType;

    @Column(name = "matched_products", columnDefinition = "TEXT[]")
    private String[] matchedProducts;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "language", length = 20)
    @Enumerated(EnumType.STRING)
    private Language language;
}
