package com.project.ai.processing.text.structure;

import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.SearchIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 14/05/2026
 * @Time: 10:26 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class MatchedIdsResolver {

    public List<String> resolve(String answer, FilteredContext context, SearchIntent intent) {
        String type = intent.getSearchType();
        log.info("[MatchedIdsResolver] Resolving for type={}", intent.getSearchType());

        return switch (type) {

            case "semantic", "comparison" -> resolveFromAnswer(answer, context);

            case "price", "category", "brand", "hybrid", "sort", "suggest" -> resolveFromContext(context);

            case "knowledge" -> List.of();

            default -> {
                log.warn("MatchedIdsResolver: unknown type '{}', falling back to context", type);
                yield resolveFromContext(context);
            }
        };
    }

    /**
     * Parse product IDs directly from LLM answer text.
     * Used when LLM selects which products to mention (semantic, comparison).
     */
    private List<String> resolveFromAnswer(String answer, FilteredContext context) {
        log.debug("[MatchedIdsResolver] Parsing IDs from answer:\n{}", answer);

        List<String> parsed = Arrays.stream(answer.split(",|\\n|\\s"))
                .map(s -> s.replaceAll("[^P0-9]", "").trim())
                .filter(s -> s.matches("P\\d{3}"))
                .distinct()
                .collect(Collectors.toList());

        if (parsed.isEmpty()) {
            log.warn("MatchedIdsResolver: could not parse IDs from answer, falling back to context");
            return resolveFromContext(context);
        }

        log.info("[MatchedIdsResolver] Parsed {} IDs from answer: {}", parsed.size(), parsed);
        return parsed;
    }

    /**
     * Take IDs directly from the filtered/sorted match list.
     * Used when Java already determined the exact result set.
     */
    private List<String> resolveFromContext(FilteredContext context) {

        List<String> ids = context.getFilteredMatches().stream()
                .map(m -> m.embedded().metadata().getString("productId"))
                .collect(Collectors.toList());

        log.info("[MatchedIdsResolver] Taking {} IDs from filtered context", ids.size());

        return ids;
    }
}
