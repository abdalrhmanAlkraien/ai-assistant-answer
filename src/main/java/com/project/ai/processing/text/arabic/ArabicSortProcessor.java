package com.project.ai.processing.text.arabic;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        // fetch from PostgreSQL directly — no vector search needed
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


        // build answer
        String answer = sorted.stream()
                .map(p -> p.getTitle() + " - " + p.getPrice() + " USD - " + p.getCategory())
                .collect(Collectors.joining("\n"));

        List<String> matchedIds = sorted.stream()
                .map(Product::getProductId)
                .toList();

        log.info("[ArabicSortProcessor] END — sorted={} direction={}",
                sorted.size(), ascending ? "asc" : "desc");

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type("sort")
                .answer(answer)
                .matchedIds(matchedIds)
                .build();
    }


    private List<Product> fetchProducts(SearchIntent intent) {
        // category filter
        if (intent.getCategory() != null && intent.getBrand() != null) {
            log.info("[ArabicSortProcessor] fetching by category={} brand={}",
                    intent.getCategory(), intent.getBrand());
            return productRepository.findActiveByCategoryAndBrand(
                    intent.getCategory(), intent.getBrand());
        }

        if (intent.getCategory() != null) {
            log.info("[ArabicSortProcessor] fetching by category={}", intent.getCategory());
            return productRepository.findActiveByCategory(intent.getCategory());
        }

        if (intent.getBrand() != null) {
            log.info("[ArabicSortProcessor] fetching by brand={}", intent.getBrand());
            return productRepository.findActiveByBrand(intent.getBrand());
        }

        // no filter — fetch all active products
        log.info("[ArabicSortProcessor] fetching all active products");
        return productRepository.findAllActive();
    }
}
