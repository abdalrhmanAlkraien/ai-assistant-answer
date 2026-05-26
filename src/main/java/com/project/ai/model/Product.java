package com.project.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:51 PM
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @Column(name = "product_id", length = 20)
    private String productId;  // P001, P002...

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "category", length = 100)
    private String category;   // "Smartphones", "Laptops"

    @Column(name = "brand", length = 100)
    private String brand;      // "Apple", "Samsung"

    @Column(name = "price")
    private Double price;

    private String description;

    @Column(name = "currency", length = 10)
    private String currency;   // "USD"

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
