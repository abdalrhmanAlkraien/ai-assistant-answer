package com.project.ai.processing.text.structure;

import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.SearchIntent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:24 PM
 */
@Service
@Log4j2
public class FilterProcessor {

    public FilteredContext filter(List<EmbeddingMatch<TextSegment>> matches, SearchIntent intent) {
        log.info("[FilterProcessor] START — type={}", intent.getSearchType());

        List<EmbeddingMatch<TextSegment>> filtered = switch (intent.getSearchType()) {
            case "price" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();
                        Double price = extractPrice(match.embedded().text());
                        if (price == null) return false;

                        boolean aboveMin = intent.getMinPrice() == null || price >= intent.getMinPrice();
                        boolean belowMax = intent.getMaxPrice() == null || price <= intent.getMaxPrice();
                        boolean priceMatch = aboveMin && belowMax;

                        // Also filter by category if present
                        boolean categoryMatch = intent.getCategory() == null ||
                                text.contains(intent.getCategory().toLowerCase());

                        return priceMatch && categoryMatch;
                    })
                    .toList();

            case "category" -> matches.stream()
                    .filter(match -> intent.getCategory() != null &&
                            match.embedded().text().toLowerCase()
                                    .contains(intent.getCategory().toLowerCase()))
                    .toList();

            case "comparison" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();

                        // Filter by brand if present
                        boolean brandMatch = true;
                        if (intent.getBrand() != null) {
                            brandMatch = Arrays.stream(intent.getBrand().split(","))
                                    .map(String::trim)
                                    .anyMatch(brand -> text.contains(brand.toLowerCase()));
                        }

                        // Filter by category if present
                        boolean categoryMatch = true;
                        if (intent.getCategory() != null) {
                            categoryMatch = text.contains(intent.getCategory().toLowerCase());
                        }

                        return brandMatch && categoryMatch;
                    })
                    .toList();

            case "brand" -> matches.stream()
                    .filter(match -> {
                        if (intent.getBrand() == null) return false;
                        String text = match.embedded().text().toLowerCase();

                        // Check brand matches
                        boolean brandMatch = Arrays.stream(intent.getBrand().split(","))
                                .map(String::trim)
                                .anyMatch(brand -> text.contains(brand.toLowerCase()));

                        // Check category if present in intent
                        if (brandMatch && intent.getCategory() != null) {
                            return text.contains(intent.getCategory().toLowerCase());
                        }

                        return brandMatch;
                    })
                    .toList();

            case "hybrid" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();
                        Double price = extractPrice(match.embedded().text());

                        // Price filter
                        boolean priceMatch = true;
                        if (price != null) {
                            boolean aboveMin = intent.getMinPrice() == null || price >= intent.getMinPrice();
                            boolean belowMax = intent.getMaxPrice() == null || price <= intent.getMaxPrice();
                            priceMatch = aboveMin && belowMax;
                        }

                        // Brand filter
                        boolean brandMatch = true;
                        if (intent.getBrand() != null) {
                            brandMatch = Arrays.stream(intent.getBrand().split(","))
                                    .map(String::trim)
                                    .anyMatch(brand -> text.contains(brand.toLowerCase()));
                        }

                        // Category filter
                        boolean categoryMatch = true;
                        if (intent.getCategory() != null) {
                            categoryMatch = text.contains(intent.getCategory().toLowerCase());
                        }

                        return priceMatch && brandMatch && categoryMatch;
                    })
                    .toList();

            case "semantic" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();
                        Double price = extractPrice(match.embedded().text());

                        boolean aboveMin = intent.getMinPrice() == null || price == null
                                || price >= intent.getMinPrice();
                        boolean belowMax = intent.getMaxPrice() == null || price == null
                                || price <= intent.getMaxPrice();
                        boolean categoryMatch = intent.getCategory() == null
                                || text.contains(intent.getCategory().toLowerCase());

                        return aboveMin && belowMax && categoryMatch;
                    })
                    .toList();

            case "suggest" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();
                        Double price = extractPrice(match.embedded().text());

                        // Price filter
                        boolean priceMatch = true;
                        if (price != null) {
                            boolean aboveMin = intent.getMinPrice() == null || price >= intent.getMinPrice();
                            boolean belowMax = intent.getMaxPrice() == null || price <= intent.getMaxPrice();
                            priceMatch = aboveMin && belowMax;
                        }

                        // Category filter — no brand filter
                        boolean categoryMatch = intent.getCategory() == null ||
                                text.contains(intent.getCategory().toLowerCase());

                        return priceMatch && categoryMatch;
                    })
                    .toList();
            case "sort" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();

                        boolean categoryMatch = intent.getCategory() == null
                                || text.contains(intent.getCategory().toLowerCase());

                        boolean brandMatch = intent.getBrand() == null
                                || Arrays.stream(intent.getBrand().split(","))
                                .map(String::trim)
                                .anyMatch(b -> text.contains(b.toLowerCase()));

                        boolean aboveMin = intent.getMinPrice() == null
                                || extractPrice(match.embedded().text()) == null
                                || extractPrice(match.embedded().text()) >= intent.getMinPrice();

                        boolean belowMax = intent.getMaxPrice() == null
                                || extractPrice(match.embedded().text()) == null
                                || extractPrice(match.embedded().text()) <= intent.getMaxPrice();

                        return categoryMatch && brandMatch && aboveMin && belowMax;
                    })
                    .toList();

            default -> matches;
        };

        String context = filtered.stream()
                .map(match -> "[" + match.embedded().metadata().getString("productId") + "] "
                        + match.embedded().text())
                .collect(Collectors.joining("\n"));


        return FilteredContext.builder()
                .context(context)
                .filteredMatches(filtered)
                .build();
    }

    private Double extractPrice(String content) {
        try {
            Pattern pattern = Pattern.compile("price:(\\d+(?:\\.\\d+)?)");  // ← lowercase, no USD
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            log.warn("[FilterProcessor] Failed to extract price: {}", content);
        }
        return null;
    }
}
