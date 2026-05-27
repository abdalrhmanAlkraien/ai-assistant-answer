package com.project.ai.service;

import com.project.ai.dto.ProductRequest;
import com.project.ai.dto.ProductUpdateRequest;
import com.project.ai.model.Product;
import com.project.ai.strategy.ecommerce.executor.EcommerceContextBuilder;
import com.project.ai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:56 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ProductService {

    private final ProductRepository productRepository;
    private final EcommerceContextBuilder ecommerceContextBuilder;

    @Transactional
    public Product create(ProductRequest request) {
        if (productRepository.existsById(request.productId())) {
            throw new IllegalArgumentException("Product '%s' already exists".formatted(request.productId()));
        }

        Product product = Product.builder()
                .productId(request.productId())
                .title(request.title())
                .category(request.category())
                .brand(request.brand())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "USD")
                .active(true)
                .build();

        Product saved = productRepository.save(product);
        ecommerceContextBuilder.invalidate();

        log.info("[ProductService] Created product id={} title={}", saved.getProductId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public List<Product> createBatch(List<ProductRequest> requests) {
        List<Product> products = requests.stream()
                .filter(r -> !productRepository.existsById(r.productId()))
                .map(r -> Product.builder()
                        .productId(r.productId())
                        .title(r.title())
                        .category(r.category())
                        .brand(r.brand())
                        .price(r.price())
                        .currency(r.currency() != null ? r.currency() : "USD")
                        .active(true)
                        .build())
                .toList();

        List<Product> saved = productRepository.saveAll(products);
        ecommerceContextBuilder.invalidate();

        log.info("[ProductService] Created {} products", saved.size());
        return saved;
    }

    @Transactional
    public Product update(String productId, ProductRequest request) {
        Product product = findById(productId);

        product.setTitle(request.title());
        product.setCategory(request.category());
        product.setBrand(request.brand());
        product.setPrice(request.price());
        product.setCurrency(request.currency() != null ? request.currency() : "USD");

        Product saved = productRepository.save(product);
        ecommerceContextBuilder.invalidate();

        log.info("[ProductService] Updated product id={}", productId);
        return saved;
    }

    @Transactional
    public void delete(String productId) {
        Product product = findById(productId);
        productRepository.delete(product);
        ecommerceContextBuilder.invalidate();
        log.info("[ProductService] Deleted product id={}", productId);
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Product findById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    @Transactional(readOnly = true)
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional
    public List<Product> updateProducts(List<ProductUpdateRequest> requests) {
        return requests.stream()
                .map(req -> {
                    Product product = productRepository.findById(req.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Product not found: " + req.getProductId()));

                    if (req.getTitle() != null)       product.setTitle(req.getTitle());
                    if (req.getCategory() != null)    product.setCategory(req.getCategory());
                    if (req.getBrand() != null)        product.setBrand(req.getBrand());
                    if (req.getPrice() != null)        product.setPrice(req.getPrice());
                    if (req.getCurrency() != null)     product.setCurrency(req.getCurrency());
                    if (req.getDescription() != null)  product.setDescription(req.getDescription());
                    if (req.getImageUrl() != null)     product.setImageUrl(req.getImageUrl());
                    if (req.getActive() != null)       product.setActive(req.getActive());

                    return productRepository.save(product);
                })
                .toList();
    }
}
