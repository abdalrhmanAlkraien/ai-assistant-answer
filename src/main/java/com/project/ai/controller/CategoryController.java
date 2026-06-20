package com.project.ai.controller;

import com.project.ai.dto.CategoryRequest;
import com.project.ai.loader.CategoryLoader;
import com.project.ai.model.Category;
import com.project.ai.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * @Time: 10:10 PM
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management and cache reload")
public class CategoryController {


    private final CategoryService categoryService;
    private final CategoryLoader categoryLoader;

    @Operation(summary = "Create a category")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Category> create(@RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @Operation(summary = "Batch create categories", description = "Create multiple categories in one request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categories created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<Category>> createList(@RequestBody @Valid List<CategoryRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createList(requests));
    }

    @Operation(summary = "Get all categories")
    @ApiResponse(responseCode = "200", description = "Categories retrieved")
    @GetMapping
    public ResponseEntity<List<Category>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @Operation(summary = "Get category by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<Category> findById(
            @Parameter(description = "Category ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @Operation(summary = "Update a category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Category> update(
            @Parameter(description = "Category ID", required = true) @PathVariable Long id,
            @RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @Operation(summary = "Delete a category")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Category ID", required = true) @PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reload category cache",
            description = "Reloads all active categories from DB into memory — use after adding new categories"
    )
    @ApiResponse(responseCode = "200", description = "Categories reloaded successfully")
    @PostMapping("/categories/reload")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<String> reloadCategories() {
        categoryLoader.reload();
        return ResponseEntity.ok("Categories reloaded");
    }
}
