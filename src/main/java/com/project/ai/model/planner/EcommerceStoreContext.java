package com.project.ai.model.planner;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 4:04 AM
 */
@Getter
@SuperBuilder
public class EcommerceStoreContext extends StoreContext {

    private final Set<String> availableCategories;  // from PostgreSQL Category table
    private final Set<String> availableBrands;      // from PostgreSQL Product table
    private final Map<String, String> categoryArabicNames;  // slug → arabic name
}
