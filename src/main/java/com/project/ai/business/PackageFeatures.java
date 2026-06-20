package com.project.ai.business;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 20/06/2026
 * @Time: 8:53 AM
 */
@Component
@RequiredArgsConstructor
public class PackageFeatures {

    private final PackageProperties packageProperties;

    // ── Regular user features per package ─────────────────────────────────────
    public static final Map<String, List<String>> FEATURES = Map.of(

            "BASIC", List.of(
                    "CHAT",
                    "USER_HISTORY",
                    "DASHBOARD",
                    "PRODUCT",
                    "DATA_MANAGEMENT",
                    "CATEGORY",
                    "EVALS",
                    "USER_MANAGEMENT"
            ),

            "GROWTH", List.of(
                    "CHAT",
                    "USER_HISTORY",
                    "DASHBOARD",
                    "GROWTH_DASHBOARD",
                    "PRODUCT",
                    "DATA_MANAGEMENT",
                    "CATEGORY",
                    "EVALS",
                    "QUERY_ANALYTICS",
                    "PEAK_HOURS",
                    "SEARCH_TYPE_BREAKDOWN",
                    "EARLY_ACCESS",
                    "USER_MANAGEMENT",
                    "SECURITY"
            ),

            "ENTERPRISE", List.of(
                    "CHAT",
                    "USER_HISTORY",
                    "ENTERPRISE_DASHBOARD",
                    "PRODUCT",
                    "DATA_MANAGEMENT",
                    "CATEGORY",
                    "EVALS",
                    "QUERY_ANALYTICS",
                    "PEAK_HOURS",
                    "SEARCH_TYPE_BREAKDOWN",
                    "EARLY_ACCESS",
                    "CUSTOM_MODELS",
                    "DEDICATED_SUPPORT",
                    "SLA",
                    "USER_MANAGEMENT",
                    "SECURITY"
            )
    );

    // ── MIGFORA_ADMIN extra features — added on top of package features ────────
    public static final List<String> MIGFORA_ADMIN_EXTRA_FEATURES = List.of(
            "PROMPT_MANAGER",
            "SYSTEM_LOOKUP"
    );

    // ── MIGFORA_ADMIN features per package ────────────────────────────────────
    public static final Map<String, List<String>> MIGFORA_ADMIN_FEATURES = Map.of(

            "BASIC", Stream.concat(
                    FEATURES.get("BASIC").stream(),
                    MIGFORA_ADMIN_EXTRA_FEATURES.stream()
            ).toList(),

            "GROWTH", Stream.concat(
                    FEATURES.get("GROWTH").stream(),
                    MIGFORA_ADMIN_EXTRA_FEATURES.stream()
            ).toList(),

            "ENTERPRISE", Stream.concat(
                    FEATURES.get("ENTERPRISE").stream(),
                    MIGFORA_ADMIN_EXTRA_FEATURES.stream()
            ).toList()
    );

    public String getActivePackage() {
        return packageProperties.getActive().toUpperCase();
    }

    public List<String> getActiveFeatures() {
        return FEATURES.getOrDefault(getActivePackage(), List.of());
    }

    public List<String> getActiveFeaturesForAdmin() {
        return MIGFORA_ADMIN_FEATURES.getOrDefault(getActivePackage(), List.of());
    }

    public boolean hasFeature(String feature) {
        return getActiveFeatures().contains(feature.toUpperCase());
    }

    public boolean hasFeatureForAdmin(String feature) {
        return getActiveFeaturesForAdmin().contains(feature.toUpperCase());
    }
}
