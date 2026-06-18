package com.project.ai.model.lookup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:15 AM
 */
@Entity
@Table(name = "system_lookup")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false)  // "search_type", "language", "status" ...
    private String type;

    @Column(name = "code", nullable = false, unique = true)
    private String code;          // "category", "brand", "price" ...

    @Column(name = "label", nullable = false)
    private String label;         // "Category", "Brand", "Price" ...

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
