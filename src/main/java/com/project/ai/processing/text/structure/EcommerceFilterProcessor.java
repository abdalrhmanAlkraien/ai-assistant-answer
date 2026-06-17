package com.project.ai.processing.text.structure;

import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.SearchIntent;
import com.project.ai.model.Product;
import com.project.ai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
    @Value("${rag.search.max-results:5}")
    private int maxResults;

    public FilteredContext filter(SearchIntent intent) {

        log.info("[EcommerceFilterProcessor] START — type={} category={} brand={} min={} max={}",
                intent.getSearchType(), intent.getCategory(), intent.getBrand(),
                intent.getMinPrice(), intent.getMaxPrice());

        List<Product> products = switch (intent.getSearchType()) {

            case "price" -> productRepository.findActiveByPriceRange(
                    intent.getMinPrice() != null ? intent.getMinPrice() : 0,
                    intent.getMaxPrice() != null ? intent.getMaxPrice() : Double.MAX_VALUE,
                    PageRequest.of(0, maxResults));

            case "category" -> intent.getBrand() != null
                    ? productRepository.findActiveByCategoryAndBrand(
                    intent.getCategory(), intent.getBrand(), PageRequest.of(0, maxResults))
                    : productRepository.findActiveByCategory(
                    intent.getCategory(), PageRequest.of(0, maxResults));

            case "brand" -> intent.getCategory() != null
                    ? productRepository.findActiveByCategoryAndBrand(
                    intent.getCategory(), intent.getBrand(), PageRequest.of(0, maxResults))
                    : productRepository.findActiveByBrand(intent.getBrand(), PageRequest.of(0, maxResults));

            case "hybrid" -> queryHybrid(intent);

            case "suggest" -> querySuggest(intent);

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
                    intent.getMaxPrice() != null ? intent.getMaxPrice() : Double.MAX_VALUE,
                    PageRequest.of(0, maxResults));
        }
        if (intent.getCategory() != null
                && (intent.getMinPrice() != null || intent.getMaxPrice() != null)) {
            return productRepository.findActiveByCategoryAndPrice(
                    intent.getCategory(),
                    intent.getMinPrice() != null ? intent.getMinPrice() : 0,
                    intent.getMaxPrice() != null ? intent.getMaxPrice() : Double.MAX_VALUE,
                    PageRequest.of(0, maxResults));
        }
        if (intent.getBrand() != null
                && (intent.getMinPrice() != null || intent.getMaxPrice() != null)) {
            return productRepository.findActiveByBrandAndPrice(
                    intent.getBrand(),
                    intent.getMinPrice() != null ? intent.getMinPrice() : 0,
                    intent.getMaxPrice() != null ? intent.getMaxPrice() : Double.MAX_VALUE,
                    PageRequest.of(0, maxResults));
        }
        if (intent.getCategory() != null && intent.getBrand() != null) {
            return productRepository.findActiveByCategoryAndBrand(
                    intent.getCategory(), intent.getBrand(), PageRequest.of(0, maxResults));
        }
        return List.of();
    }

    private List<Product> querySuggest(SearchIntent intent) {
        String excludedBrand = intent.getExcludedBrand();
        String category      = intent.getCategory();
        Double maxPrice      = intent.getMaxPrice();

        log.info("[EcommerceFilterProcessor] suggest — category={} excludedBrand={} maxPrice={}",
                category, excludedBrand, maxPrice);

        // category + excluded brand + price
        if (category != null && excludedBrand != null && maxPrice != null) {
            return productRepository.findActiveByCategoryExcludingBrandWithMaxPrice(
                    category, excludedBrand, maxPrice, PageRequest.of(0, maxResults));
        }
        // category + excluded brand
        if (category != null && excludedBrand != null) {
            return productRepository.findActiveByCategoryExcludingBrand(
                    category, excludedBrand, PageRequest.of(0, maxResults));
        }
        // category only
        if (category != null) {
            return productRepository.findActiveByCategory(
                    category, PageRequest.of(0, maxResults));
        }
        // no category, no brand → return empty (too vague)
        return List.of();
    }

    private FilteredContext buildContext(List<Product> products) {
        String context = products.stream()
                .map(p -> p.getTitle() + " - $" + p.getPrice() + " - " + p.getCategory())
                .collect(Collectors.joining("\n"));

        return FilteredContext.builder()
                .products(products)
                .filteredMatches(List.of())
                .context(context)
                .build();
    }

}
