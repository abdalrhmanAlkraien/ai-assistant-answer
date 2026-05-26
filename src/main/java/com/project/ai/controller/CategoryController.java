package com.project.ai.controller;

import com.project.ai.dto.CategoryRequest;
import com.project.ai.loader.CategoryLoader;
import com.project.ai.model.Category;
import com.project.ai.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * @Time: 10:10 PM
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {


    private final CategoryService categoryService;
    private final CategoryLoader categoryLoader;

    @PostMapping
    public ResponseEntity<Category> create(@RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Category>> createList(@RequestBody @Valid List<CategoryRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createList(requests));
    }

    @GetMapping
    public ResponseEntity<List<Category>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> update(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/categories/reload")
    public ResponseEntity<String> reloadCategories() {
        categoryLoader.reload();
        return ResponseEntity.ok("Categories reloaded");
    }
}
