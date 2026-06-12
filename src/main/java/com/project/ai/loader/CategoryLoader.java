package com.project.ai.loader;

import com.project.ai.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 6:24 AM
 */
@Component
@RequiredArgsConstructor
@Log4j2
@Getter
public class CategoryLoader {

    private final CategoryRepository categoryRepository;

    private final Set<String> categorySlugs = ConcurrentHashMap.newKeySet();
    private final Map<String, String> arabicToSlug = new ConcurrentHashMap<>();
    private final Map<String, String> descriptionKeywordToSlug = new ConcurrentHashMap<>();

    @PostConstruct
    public void load() {
        log.info("[CategoryLoader] Loading categories from DB");

        categoryRepository.findAllActiveWithArabicName().forEach(c -> {
            if (c.getSlug() != null) {
                categorySlugs.add(c.getSlug().toLowerCase());
            }
            if (c.getNameArabic() != null && c.getSlug() != null) {
                arabicToSlug.put(c.getNameArabic().trim(), c.getSlug().toLowerCase());
            }
            if (c.getDescription() != null && c.getSlug() != null) {
                Arrays.stream(c.getDescription().toLowerCase().split("[\\s,]+"))
                        .filter(word -> word.length() > 3)
                        .forEach(word -> descriptionKeywordToSlug.putIfAbsent(word, c.getSlug().toLowerCase()));
            }
        });

        log.info("[CategoryLoader] Loaded {} slugs, {} arabic mappings, {} description keywords",
                categorySlugs.size(), arabicToSlug.size(), descriptionKeywordToSlug.size());
    }

    public void reload() {
        categorySlugs.clear();
        arabicToSlug.clear();
        descriptionKeywordToSlug.clear();
        load();
        log.info("[CategoryLoader] Reloaded categories");
    }
}
