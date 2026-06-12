package com.project.ai.processing.text.arabic;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.model.Product;
import com.project.ai.processing.ChatProcessor;
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
 * @Date: 16/05/2026
 * @Time: 11:24 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ArabicSortProcessor implements ChatProcessor {

    private final ProductRepository productRepository;

    @Value("${rag.search.max-results:5}")
    private int maxResults;

    @Override
    public boolean supports(String searchType) {
        return "sort".equals(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        SearchIntent intent = request.getSearchIntent();
        log.info("[ArabicSortProcessor] START — direction={} category={} brand={}",
                intent.getSortDirection(), intent.getCategory(), intent.getBrand());

        boolean ascending = !"desc".equals(intent.getSortDirection());

        boolean singleResult = intent.isSingleResult();

        log.info("[ArabicSortProcessor] singleResult={} ascending={}", singleResult, ascending);

        List<Product> products = fetchProducts(intent);

        if (products.isEmpty()) {
            log.info("[ArabicSortProcessor] No products found for sort");
            return ProcessingResult.builder()
                    .enrichedQuestion(intent.getSemanticQuery())
                    .type("sort")
                    .answer("لم يتم العثور على منتجات للترتيب.")
                    .matchedIds(List.of())
                    .build();
        }

        List<Product> sorted = products.stream()
                .sorted((a, b) -> ascending
                        ? Double.compare(a.getPrice(), b.getPrice())
                        : Double.compare(b.getPrice(), a.getPrice()))
                .toList();

        // ── Apply limit if single result query ────────────────────────────────
        List<Product> result = singleResult ? List.of(sorted.get(0)) : sorted;

        String answer = result.stream()
                .map(p -> p.getTitle() + " - $" + p.getPrice() + " - " + p.getCategory())
                .collect(Collectors.joining("\n"));

        List<String> matchedIds = result.stream()
                .map(Product::getProductId)
                .toList();

        log.info("[ArabicSortProcessor] END — returned={} direction={}",
                result.size(), ascending ? "asc" : "desc");

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type("sort")
                .answer(answer)
                .matchedIds(matchedIds)
                .build();
    }

    private List<Product> fetchProducts(SearchIntent intent) {
        if (intent.getCategory() != null && intent.getBrand() != null) {
            log.info("[ArabicSortProcessor] fetching by category={} brand={}",
                    intent.getCategory(), intent.getBrand());
            return productRepository.findActiveByCategoryAndBrand(
                    intent.getCategory(), intent.getBrand(), PageRequest.of(0, maxResults));
        }

        if (intent.getCategory() != null) {
            log.info("[ArabicSortProcessor] fetching by category={}", intent.getCategory());
            return productRepository.findActiveByCategory(intent.getCategory(), PageRequest.of(0, maxResults));
        }

        if (intent.getBrand() != null) {
            log.info("[ArabicSortProcessor] fetching by brand={}", intent.getBrand());
            return productRepository.findActiveByBrand(intent.getBrand(), PageRequest.of(0, maxResults));
        }

        log.info("[ArabicSortProcessor] fetching all active products");
        return productRepository.findAllActive(PageRequest.of(0, maxResults));
    }

    private boolean enrichedQuestionImpliesSingle(String enriched) {
        if (enriched == null) return false;
        String lower = enriched.toLowerCase();
        // "what is the cheapest" / "cheapest laptop" patterns
        return lower.startsWith("cheapest")
                || lower.startsWith("most expensive")
                || lower.contains("what is the cheapest")
                || lower.contains("what is the most expensive")
                || lower.contains("which is the cheapest")
                || lower.contains("which is the most expensive");
    }
}
