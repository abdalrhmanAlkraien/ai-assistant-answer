package com.project.ai.repository;

import com.project.ai.model.evels.Evaluation;
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
 * @Time: 1:40 AM
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    @Query(value = """
    SELECT * FROM evals.evaluation e
    WHERE (:status IS NULL OR e.status = :status)
      AND (:resultType IS NULL OR e.result_type = :resultType)
      AND (:lang IS NULL OR :lang = ANY(e.lang))
    ORDER BY e.created_at DESC
    """,
            countQuery = """
    SELECT COUNT(*) FROM evals.evaluation e
    WHERE (:status IS NULL OR e.status = :status)
      AND (:resultType IS NULL OR e.result_type = :resultType)
      AND (:lang IS NULL OR :lang = ANY(e.lang))
    """,
            nativeQuery = true)
    Page<Evaluation> findAllWithFilters(
            @Param("status") String status,
            @Param("resultType") String resultType,
            @Param("lang") String lang,
            Pageable pageable);

    Optional<Evaluation> findTopByOrderByCreatedAtDesc();


    // EvaluationRepository.java
    @Query("""
    SELECT DISTINCT e FROM Evaluation e
    LEFT JOIN FETCH e.evaluationTypes et
    LEFT JOIN FETCH et.contexts
    LEFT JOIN FETCH e.failures
    WHERE e.id = :id
""")
    Optional<Evaluation> findByIdWithDetails(@Param("id") Long id);
}
