package com.project.ai.model.planner;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:52 PM
 */
@SuperBuilder
@Getter
public class StoreContext {

    private final Double minPrice;
    private final Double maxPrice;
}
