package com.project.ai.repository;

import com.project.ai.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:52 PM
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.active = true ORDER BY p.brand")
    List<String> findAllActiveBrands();

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.active = true ORDER BY p.category")
    List<String> findAllActiveCategories();

    @Query("SELECT MIN(p.price) FROM Product p WHERE p.active = true")
    Double findMinPrice();

    @Query("SELECT MAX(p.price) FROM Product p WHERE p.active = true")
    Double findMaxPrice();

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.price ASC")
    List<Product> findAllActive();

    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.category) = LOWER(:category)")
    List<Product> findActiveByCategory(@Param("category") String category);

    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.brand) = LOWER(:brand)")
    List<Product> findActiveByBrand(@Param("brand") String brand);

    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.category) = LOWER(:category) AND LOWER(p.brand) = LOWER(:brand)")
    List<Product> findActiveByCategoryAndBrand(
            @Param("category") String category,
            @Param("brand") String brand);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.price >= :min AND p.price <= :max")
    List<Product> findActiveByPriceRange(
            @Param("min") Double min,
            @Param("max") Double max);

    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.category) = LOWER(:category) AND p.price >= :min AND p.price <= :max")
    List<Product> findActiveByCategoryAndPrice(
            @Param("category") String category,
            @Param("min") Double min,
            @Param("max") Double max);

    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.brand) = LOWER(:brand) AND p.price >= :min AND p.price <= :max")
    List<Product> findActiveByBrandAndPrice(
            @Param("brand") String brand,
            @Param("min") Double min,
            @Param("max") Double max);

    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.category) = LOWER(:category) AND LOWER(p.brand) = LOWER(:brand) AND p.price >= :min AND p.price <= :max")
    List<Product> findActiveByAllFilters(
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("min") Double min,
            @Param("max") Double max);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.productId IN :ids")
    List<Product> findActiveByProductIds(@Param("ids") List<String> ids);
}
