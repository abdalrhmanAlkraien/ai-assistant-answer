package com.project.ai.repository;

import com.project.ai.model.evels.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:54 AM
 */
@Repository
public interface ContextRepository extends JpaRepository<Context, Long> {

    @Query(value = """
    SELECT c.* FROM evals.context c
    JOIN evals.evaluation_type et ON et.id = c.evaluation_type_id
    WHERE et.evaluation_id = :evalId
      AND (:language IS NULL OR UPPER(c.language) = UPPER(:language))
      AND (:status IS NULL OR UPPER(c.status) = UPPER(:status))
      AND (:searchType IS NULL OR c.search_type = :searchType)
    ORDER BY c.id ASC
    """,
            countQuery = """
    SELECT COUNT(*) FROM evals.context c
    JOIN evals.evaluation_type et ON et.id = c.evaluation_type_id
    WHERE et.evaluation_id = :evalId
      AND (:language IS NULL OR UPPER(c.language) = UPPER(:language))
      AND (:status IS NULL OR UPPER(c.status) = UPPER(:status))
      AND (:searchType IS NULL OR c.search_type = :searchType)
    """,
            nativeQuery = true)
    Page<Context> findByEvaluationIdWithFilters(
            @Param("evalId") Long evalId,
            @Param("language") String language,
            @Param("status") String status,
            @Param("searchType") String searchType,
            Pageable pageable);

    @Query(value = """
    SELECT c.* FROM evals.context c
    JOIN evals.evaluation_type et ON et.id = c.evaluation_type_id
    WHERE c.id = :id
      AND et.evaluation_id = :evalId
    """, nativeQuery = true)
    Optional<Context> findByIdAndEvaluationTypeEvaluationId(
            @Param("id") Long id,
            @Param("evalId") Long evalId);

}
