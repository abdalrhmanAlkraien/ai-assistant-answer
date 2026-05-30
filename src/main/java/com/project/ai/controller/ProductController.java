package com.project.ai.controller;

import com.project.ai.dto.BulkUploadResponse;
import com.project.ai.dto.IndexResponse;
import com.project.ai.dto.ProductRequest;
import com.project.ai.dto.ProductUpdateRequest;
import com.project.ai.model.Product;
import com.project.ai.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:59 PM
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management")
public class ProductController {


    private final ProductService productService;

    @Operation(summary = "Create a single product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody @Valid ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @Operation(summary = "Batch create products", description = "Create multiple products in one request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Products created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    @PostMapping("/batch")
    public ResponseEntity<List<Product>> createBatch(@RequestBody @Valid List<ProductRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createBatch(requests));
    }

    @Operation(summary = "Get all products", description = "Returns paginated list of all products. Default page size is 20, sorted by title.")
    @ApiResponse(responseCode = "200", description = "Products retrieved")
    @GetMapping
    public ResponseEntity<Page<Product>> findAll(
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(productService.findAll(pageable));
    }

    @Operation(summary = "Get product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/{productId}")
    public ResponseEntity<Product> findById(
            @Parameter(description = "Product ID e.g. P001", required = true) @PathVariable String productId) {
        return ResponseEntity.ok(productService.findById(productId));
    }

    @Operation(summary = "Update a product", description = "Update a single product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @PutMapping("/{productId}")
    public ResponseEntity<Product> update(
            @Parameter(description = "Product ID e.g. P001", required = true) @PathVariable String productId,
            @RequestBody @Valid ProductRequest request) {
        return ResponseEntity.ok(productService.update(productId, request));
    }

    @Operation(summary = "Batch update products", description = "Update multiple products at once — description, imageUrl, price, active status etc.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    @PutMapping("/batch")
    public ResponseEntity<List<Product>> updateProducts(
            @RequestBody List<ProductUpdateRequest> requests) {
        return ResponseEntity.ok(productService.updateProducts(requests));
    }

    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Product ID e.g. P001", required = true) @PathVariable String productId) {
        productService.delete(productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Batch delete products", description = "Delete multiple products by ID list")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Products deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteProducts(@RequestBody List<String> productIds) {
        productService.deleteProducts(productIds);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Index all active products",
            description = "Embeds and indexes all active products into ChromaDB for semantic search")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indexing completed"),
            @ApiResponse(responseCode = "500", description = "Indexing failed", content = @Content)
    })
    @PostMapping("/index")
    public ResponseEntity<IndexResponse> indexAll() {
        return ResponseEntity.ok(productService.indexAllProducts());
    }

    @Operation(
            summary = "Upload products from JSON file",
            description = "Upload a JSON file containing an array of products. Skips existing product IDs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload completed"),
            @ApiResponse(responseCode = "400", description = "Invalid file or format", content = @Content),
            @ApiResponse(responseCode = "500", description = "Upload failed", content = @Content)
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResponse> uploadJson(
            @Parameter(description = "JSON file containing array of products", required = true)
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(productService.uploadProductsJson(file));
    }

    @Operation(summary = "Toggle product status", description = "Activate or deactivate a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product status updated"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @PatchMapping("/{productId}/status")
    public ResponseEntity<Product> toggleStatus(
            @Parameter(description = "Product ID e.g. P001", required = true) @PathVariable String productId,
            @Parameter(description = "true to activate, false to deactivate", required = true) @RequestParam boolean active) {
        return ResponseEntity.ok(productService.toggleStatus(productId, active));
    }

    @Operation(
            summary = "Clear product index",
            description = "Removes all product embeddings from ChromaDB vector store. Use before re-indexing all products."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Index cleared successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to clear index", content = @Content)
    })
    @DeleteMapping("/index")
    public ResponseEntity<String> clearIndex() {
        productService.clearProductIndex();
        return ResponseEntity.ok("Product index cleared successfully");
    }

}
