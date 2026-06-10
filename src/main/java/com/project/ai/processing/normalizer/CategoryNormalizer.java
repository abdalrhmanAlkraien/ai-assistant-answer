package com.project.ai.processing.normalizer;

import com.project.ai.loader.CategoryLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

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

        String trimmed = category.trim();
        String lower = trimmed.toLowerCase();

        // check Arabic map first
        String fromArabic = categoryLoader.getArabicToSlug().get(trimmed);
        if (fromArabic != null) {
            log.info("[CategoryNormalizer] Arabic '{}' → '{}'", trimmed, fromArabic);
            return fromArabic;
        }

        // check if already a valid slug
        if (categoryLoader.getCategorySlugs().contains(lower)) {
            return lower;
        }

        // partial match — find slug that contains the input
        String partial = categoryLoader.getCategorySlugs().stream()
                .filter(slug -> {
                    if (slug.equals(lower)) return true;
                    if (slug.contains(lower)) return true;
                    if (lower.contains(slug) && slug.length() > lower.length() * 0.7) return true;
                    return false;
                })
                .findFirst()
                .orElse(null);

        if (partial != null) {
            log.info("[CategoryNormalizer] partial match '{}' → '{}'", lower, partial);
            return partial;
        }

        log.warn("[CategoryNormalizer] unknown category '{}' — returning as-is", lower);
        return lower;
    }
}
