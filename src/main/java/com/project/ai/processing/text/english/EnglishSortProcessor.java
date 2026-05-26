package com.project.ai.processing.text.english;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.model.Product;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 14/05/2026
 * @Time: 11:23 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EnglishSortProcessor implements ChatProcessor {

    private final ProductRepository productRepository;

    @Override
    public boolean supports(String searchType) {
        return "sort".equals(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {
        SearchIntent intent = request.getSearchIntent();
        log.info("[EnglishSortProcessor] START — direction={} category={} brand={}",
                intent.getSortDirection(), intent.getCategory(), intent.getBrand());

        boolean ascending = !"desc".equals(intent.getSortDirection());

        List<Product> products = fetchProducts(intent);

        if (products.isEmpty()) {
            log.info("[EnglishSortProcessor] No products found for sort");
            return ProcessingResult.builder()
                    .enrichedQuestion(intent.getSemanticQuery())
                    .type("sort")
                    .answer("No products found to sort.")
                    .matchedIds(List.of())
                    .build();
        }

        List<Product> sorted = products.stream()
                .sorted((a, b) -> ascending
                        ? Double.compare(a.getPrice(), b.getPrice())
                        : Double.compare(b.getPrice(), a.getPrice()))
                .toList();

        String answer = sorted.stream()
                .map(p -> p.getTitle() + " - " + p.getPrice() + " USD - " + p.getCategory())
                .collect(Collectors.joining("\n"));

        List<String> matchedIds = sorted.stream()
                .map(Product::getProductId)
                .toList();

        log.info("[EnglishSortProcessor] END — sorted={} direction={}",
                sorted.size(), ascending ? "asc" : "desc");

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type("sort")
                .answer(answer)
                .matchedIds(matchedIds)
                .build();
    }


    private List<Product> fetchProducts(SearchIntent intent) {
        if (intent.getCategory() != null && intent.getBrand() != null) {
            log.info("[EnglishSortProcessor] fetching by category={} brand={}",
                    intent.getCategory(), intent.getBrand());
            return productRepository.findActiveByCategoryAndBrand(
                    intent.getCategory(), intent.getBrand());
        }

        if (intent.getCategory() != null) {
            log.info("[EnglishSortProcessor] fetching by category={}", intent.getCategory());
            return productRepository.findActiveByCategory(intent.getCategory());
        }

        if (intent.getBrand() != null) {
            log.info("[EnglishSortProcessor] fetching by brand={}", intent.getBrand());
            return productRepository.findActiveByBrand(intent.getBrand());
        }

        log.info("[EnglishSortProcessor] fetching all active products");
        return productRepository.findAllActive();
    }
}
