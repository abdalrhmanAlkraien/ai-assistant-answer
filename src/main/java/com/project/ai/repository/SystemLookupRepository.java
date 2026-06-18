package com.project.ai.repository;

import com.project.ai.model.lookup.SystemLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:17 AM
 */
@Repository
public interface SystemLookupRepository extends JpaRepository<SystemLookup, Long> {
    List<SystemLookup> findByTypeAndActiveTrueOrderBySortOrderAsc(String type);
    List<SystemLookup> findByTypeOrderBySortOrderAsc(String type);
    List<SystemLookup> findAllByOrderByTypeAscSortOrderAsc();
    boolean existsByTypeAndCode(String type, String code);
    boolean existsByTypeAndCodeAndIdNot(String type, String code, Long id);
}
