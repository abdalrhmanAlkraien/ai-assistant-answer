package com.project.ai.processing.text.structure;

import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.SearchIntent;
import com.project.ai.model.Product;
import com.project.ai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 5:05 AM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EcommerceFilterProcessor {

    private final ProductRepository productRepository;


    public FilteredContext filter(SearchIntent intent) {

        log.info("[EcommerceFilterProcessor] START — type={} category={} brand={} min={} max={}",
                intent.getSearchType(), intent.getCategory(), intent.getBrand(),
                intent.getMinPrice(), intent.getMaxPrice());

        List<Product> products = switch (intent.getSearchType()) {

            case "price" -> productRepository.findActiveByPriceRange(
                    intent.getMinPrice() != null ? intent.getMinPrice() : 0,
                    intent.getMaxPrice() != null ? intent.getMaxPrice() : Double.MAX_VALUE);

            case "category" -> productRepository.findActiveByCategory(
                    intent.getCategory());

            case "brand" -> intent.getCategory() != null
                    ? productRepository.findActiveByCategoryAndBrand(
                    intent.getCategory(), intent.getBrand())
                    : productRepository.findActiveByBrand(intent.getBrand());

            case "hybrid" -> queryHybrid(intent);

            default -> {
                log.warn("[EcommerceFilterProcessor] unexpected type='{}' for Tier0",
                        intent.getSearchType());
                yield List.of();
            }
        };

        log.info("[EcommerceFilterProcessor] END — found={}", products.size());

        return buildContext(products);
    }

    private List<Product> queryHybrid(SearchIntent intent) {
        if (intent.getCategory() != null && intent.getBrand() != null
                && (intent.getMinPrice() != null || intent.getMaxPrice() != null)) {
            return productRepository.findActiveByAllFilters(
                    intent.getCategory(),
                    intent.getBrand(),
                    intent.getMinPrice() != null ? intent.getMinPrice() : 0,
                    intent.getMaxPrice() != null ? intent.getMaxPrice() : Double.MAX_VALUE);
        }
        if (intent.getCategory() != null
                && (intent.getMinPrice() != null || intent.getMaxPrice() != null)) {
            return productRepository.findActiveByCategoryAndPrice(
                    intent.getCategory(),
                    intent.getMinPrice() != null ? intent.getMinPrice() : 0,
                    intent.getMaxPrice() != null ? intent.getMaxPrice() : Double.MAX_VALUE);
        }
        if (intent.getBrand() != null
                && (intent.getMinPrice() != null || intent.getMaxPrice() != null)) {
            return productRepository.findActiveByBrandAndPrice(
                    intent.getBrand(),
                    intent.getMinPrice() != null ? intent.getMinPrice() : 0,
                    intent.getMaxPrice() != null ? intent.getMaxPrice() : Double.MAX_VALUE);
        }
        if (intent.getCategory() != null && intent.getBrand() != null) {
            return productRepository.findActiveByCategoryAndBrand(
                    intent.getCategory(), intent.getBrand());
        }
        return List.of();
    }


    private FilteredContext buildContext(List<Product> products) {
        String context = products.stream()
                .map(p -> "[" + p.getProductId() + "] "
                        + "Title: " + p.getTitle()
                        + " Category: " + p.getCategory()
                        + " Price: " + p.getPrice() + " USD"
                        + " Description: " + p.getDescription())
                .collect(Collectors.joining("\n"));

        return FilteredContext.builder()
                .products(products)
                .filteredMatches(List.of())
                .context(context)
                .build();
    }

}
