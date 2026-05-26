package com.project.ai.repository;

import com.project.ai.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:05 PM
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByName(String name);

    List<Category> findByActiveTrue();

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    @Query("""
            SELECT c FROM Category c
            WHERE c.active = true
            AND (
                LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.nameArabic) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    List<Category> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT c.slug FROM Category c WHERE c.active = true")
    List<String> findAllActiveSlugs();

    @Query("SELECT c.name FROM Category c WHERE c.active = true")
    List<String> findAllActiveNames();

    @Query("SELECT c FROM Category c WHERE c.active = true AND c.nameArabic IS NOT NULL")
    List<Category> findAllActiveWithArabicName();
}
