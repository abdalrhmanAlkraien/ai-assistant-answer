package com.project.ai.service;

import com.project.ai.dto.CategoryRequest;
import com.project.ai.model.Category;
import com.project.ai.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:06 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category create(CategoryRequest request) {
        if (categoryRepository.existsBySlug(buildSlug(request.name()))) {
            throw new IllegalArgumentException("Category '%s' already exists".formatted(request.name()));
        }

        Category category = Category.builder()
                .name(request.name())
                .slug(buildSlug(request.name()))
                .nameArabic(request.nameArabic())
                .description(request.description())
                .active(true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("[CategoryService] Created category id={} slug={}", saved.getId(), saved.getSlug());
        return saved;
    }

    @Transactional
    public List<Category> createList(List<CategoryRequest> requests) {
        List<Category> categories = requests.stream()
                .filter(r -> !categoryRepository.existsBySlug(buildSlug(r.name())))
                .map(r -> Category.builder()
                        .name(r.name())
                        .slug(buildSlug(r.name()))
                        .nameArabic(r.nameArabic())
                        .description(r.description())
                        .active(true)
                        .build())
                .toList();

        List<Category> saved = categoryRepository.saveAll(categories);
        log.info("[CategoryService] Created {} categories", saved.size());
        return saved;
    }



    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);

        String newSlug = buildSlug(request.name());
        if (!category.getSlug().equals(newSlug) && categoryRepository.existsBySlug(newSlug)) {
            throw new IllegalArgumentException("Category '%s' already exists".formatted(request.name()));
        }

        category.setName(request.name());
        category.setSlug(newSlug);
        category.setNameArabic(request.nameArabic());
        category.setDescription(request.description());

        log.info("[CategoryService] Updated category id={} slug={}", id, newSlug);
        return categoryRepository.save(category);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        categoryRepository.delete(category);
        log.info("[CategoryService] Deleted category id={}", id);
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String buildSlug(String name) {
        return name.toLowerCase().trim();
    }
}
