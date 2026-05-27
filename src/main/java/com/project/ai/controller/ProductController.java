package com.project.ai.controller;

import com.project.ai.dto.ProductRequest;
import com.project.ai.dto.ProductUpdateRequest;
import com.project.ai.model.Product;
import com.project.ai.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:59 PM
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody @Valid ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Product>> createBatch(@RequestBody @Valid List<ProductRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createBatch(requests));
    }

    @GetMapping
    public ResponseEntity<Page<Product>> findAll(
            @PageableDefault(size = 20, sort = "title") Pageable pageable
    ) {
        return ResponseEntity.ok(productService.findAll(pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> findById(@PathVariable String productId) {
        return ResponseEntity.ok(productService.findById(productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Product> update(
            @PathVariable String productId,
            @RequestBody @Valid ProductRequest request
    ) {
        return ResponseEntity.ok(productService.update(productId, request));
    }

    @PutMapping("/batch")
    public ResponseEntity<List<Product>> updateProducts(
            @RequestBody List<ProductUpdateRequest> requests) {
        return ResponseEntity.ok(productService.updateProducts(requests));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable String productId) {
        productService.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
