package com.project.ai.processing.normalizer;

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

    private static final Set<String> KNOWN_CATEGORIES = Set.of(
            "laptops", "gaming laptops", "smartphones", "headphones",
            "earbuds", "speakers", "tvs", "gaming", "wearables",
            "cameras", "accessories", "smart home", "storage",
            "monitors", "kitchen", "appliances", "shoes", "clothing"
    );

    private static final Map<String, String> ARABIC_TO_ENGLISH = Map.ofEntries(
            // Arabic variants → English DB value
            Map.entry("لابتوب",           "laptops"),
            Map.entry("لاب توب",          "laptops"),
            Map.entry("لاب توبات",        "laptops"),
            Map.entry("حاسوب محمول",      "laptops"),
            Map.entry("لابتوبات",         "laptops"),
            Map.entry("لابتوب ألعاب",     "gaming laptops"),
            Map.entry("جيمينج لابتوب",    "gaming laptops"),
            Map.entry("جوال",             "smartphones"),
            Map.entry("هاتف",             "smartphones"),
            Map.entry("موبايل",           "smartphones"),
            Map.entry("هواتف ذكية",       "smartphones"),
            Map.entry("سماعات رأس",       "headphones"),
            Map.entry("سماعات",           "headphones"),
            Map.entry("سماعات أذن",       "earbuds"),
            Map.entry("إيربودز",          "earbuds"),
            Map.entry("مكبر صوت",         "speakers"),
            Map.entry("سبيكر",            "speakers"),
            Map.entry("تلفزيون",          "tvs"),
            Map.entry("شاشة تلفاز",       "tvs"),
            Map.entry("ألعاب فيديو",      "gaming"),
            Map.entry("كونسول",           "gaming"),
            Map.entry("ساعة ذكية",        "wearables"),
            Map.entry("أجهزة قابلة للارتداء", "wearables"),
            Map.entry("كاميرا",           "cameras"),
            Map.entry("إكسسوارات",        "accessories"),
            Map.entry("منزل ذكي",         "smart home"),
            Map.entry("تخزين",            "storage"),
            Map.entry("شاشة كمبيوتر",     "monitors"),
            Map.entry("مطبخ",             "kitchen"),
            Map.entry("أجهزة منزلية",     "appliances"),
            Map.entry("أحذية",            "shoes"),
            Map.entry("ملابس",            "clothing")
    );

    public String normalize(String category) {
        if (category == null) return null;

        // Already English — return as-is lowercased
        String lower = category.toLowerCase().trim();

        // Check Arabic map
        String normalized = ARABIC_TO_ENGLISH.get(category.trim());
        if (normalized != null) {
            log.warn("[CategoryNormalizer] Arabic category '{}' normalized to '{}'", category, normalized);
            return normalized;
        }

        return lower; // already English
    }
}
