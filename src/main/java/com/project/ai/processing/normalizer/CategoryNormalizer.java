package com.project.ai.processing.normalizer;

import com.project.ai.loader.CategoryLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 21/05/2026
 * @Time: 10:42 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class CategoryNormalizer {


    private final CategoryLoader categoryLoader;

    public String normalize(String category) {
        if (category == null) return null;

        // if LLM returned comma-separated synonyms, try each one
        if (category.contains(",")) {
            for (String candidate : category.split(",")) {
                String result = normalizeSingle(candidate.trim());
                if (categoryLoader.getCategorySlugs().contains(result)) {
                    log.info("[CategoryNormalizer] synonym match '{}' → '{}'", category, result);
                    return result;
                }
            }
        }

        return normalizeSingle(category);
    }

    private String normalizeSingle(String category) {
        if (category == null) return null;

        String trimmed = category.trim();
        String lower = trimmed.toLowerCase();

        // check Arabic map first
        String fromArabic = categoryLoader.getArabicToSlug().get(trimmed);
        if (fromArabic != null) {
            log.info("[CategoryNormalizer] Arabic '{}' → '{}'", trimmed, fromArabic);
            return fromArabic;
        }

        // exact slug match
        if (categoryLoader.getCategorySlugs().contains(lower)) {
            return lower;
        }

        // partial match
        String partial = categoryLoader.getCategorySlugs().stream()
                .filter(slug -> slug.equals(lower)
                        || slug.contains(lower)
                        || (lower.contains(slug) && slug.length() > lower.length() * 0.7))
                .findFirst()
                .orElse(null);

        if (partial != null) {
            log.info("[CategoryNormalizer] partial match '{}' → '{}'", lower, partial);
            return partial;
        }

        // check description keywords
        String fromDescription = categoryLoader.getDescriptionKeywordToSlug().entrySet().stream()
                .filter(e -> {
                    String keyword = e.getKey();
                    int idx = lower.indexOf(keyword);
                    if (idx < 0) return false;
                    // check word boundaries
                    boolean beforeOk = idx == 0 || !Character.isLetterOrDigit(lower.charAt(idx - 1));
                    boolean afterOk = idx + keyword.length() == lower.length()
                            || !Character.isLetterOrDigit(lower.charAt(idx + keyword.length()));
                    return beforeOk && afterOk;
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (fromDescription != null) {
            log.info("[CategoryNormalizer] description keyword match '{}' → '{}'", lower, fromDescription);
            return fromDescription;
        }

        log.warn("[CategoryNormalizer] unknown category '{}' — returning as-is", lower);
        return lower;
    }
}
