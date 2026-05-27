package com.project.ai.service;

import com.project.ai.dto.ProductSummary;
import com.project.ai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 7:57 AM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ProductEnrichmentService {

    private final ProductRepository productRepository;

    public List<ProductSummary> enrich(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();

            return productRepository.findActiveByProductIds(productIds).stream()
                .map(p -> ProductSummary.builder()
                        .id(p.getProductId())
                        .name(p.getTitle())
                        .price("$" + p.getPrice())
                        .category(p.getCategory())
                        .brand(p.getBrand())
                        .description(p.getDescription())
                        .imageUrl(p.getImageUrl())
                        .build())
                .toList();
    }
}
