package com.project.ai.repository;

import com.project.ai.model.evels.EvaluationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:41 AM
 */
@Repository
public interface EvaluationTypeRepository extends JpaRepository<EvaluationType, Long> {
    List<EvaluationType> findByEvaluationId(Long evaluationId);
}
