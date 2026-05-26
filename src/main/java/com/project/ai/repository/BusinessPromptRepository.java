package com.project.ai.repository;

import com.project.ai.model.prompt.BusinessPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 10:52 PM
 */
@Repository
public interface BusinessPromptRepository extends JpaRepository<BusinessPrompt, Long> {
    Optional<BusinessPrompt> findByBusinessNameAndPromptKeyAndIsActiveTrue(
            String businessName, String promptKey);

    List<BusinessPrompt> findAllByBusinessNameAndIsActiveTrue(final String businessName);
}
