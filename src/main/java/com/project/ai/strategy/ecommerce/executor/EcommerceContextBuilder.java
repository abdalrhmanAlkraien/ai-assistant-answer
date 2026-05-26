package com.project.ai.strategy.ecommerce.executor;

import com.project.ai.model.Category;
import com.project.ai.model.planner.EcommerceStoreContext;
import com.project.ai.model.planner.StoreContext;
import com.project.ai.processing.planner.ContextBuilder;
import com.project.ai.repository.CategoryRepository;
import com.project.ai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:53 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EcommerceContextBuilder implements ContextBuilder {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    private StoreContext cachedContext;
    private Instant cacheTime;
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public EcommerceStoreContext build() {
        if (isCacheValid()) {
            log.debug("[EcommerceContextBuilder] Returning cached context");
            return (EcommerceStoreContext) cachedContext;
        }

        log.info("[EcommerceContextBuilder] Building fresh store context");

        Set<String> categories = new HashSet<>(categoryRepository.findAllActiveSlugs());
        Map<String, String> arabicNames = categoryRepository.findAllActiveWithArabicName()
                .stream()
                .collect(Collectors.toMap(
                        Category::getSlug,
                        Category::getNameArabic
                ));
        Set<String> brands  = new HashSet<>(productRepository.findAllActiveBrands());
        Double minPrice     = productRepository.findMinPrice();
        Double maxPrice     = productRepository.findMaxPrice();

        cachedContext = EcommerceStoreContext.builder()
                .availableCategories(categories)
                .categoryArabicNames(arabicNames)
                .availableBrands(brands)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();

        cacheTime = Instant.now();

        log.info("[EcommerceContextBuilder] Context built — categories={} brands={} priceRange={}-{}",
                categories.size(), brands.size(), minPrice, maxPrice);

        return (EcommerceStoreContext) cachedContext;
    }

    @Override
    public void invalidate() {
        cachedContext = null;
        cacheTime = null;
        log.info("[EcommerceContextBuilder] Cache invalidated");
    }

    private boolean isCacheValid() {
        return cachedContext != null
                && cacheTime != null
                && Duration.between(cacheTime, Instant.now()).compareTo(CACHE_TTL) < 0;
    }
}
