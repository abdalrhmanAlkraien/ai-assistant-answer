package com.project.ai.repository;

import com.project.ai.model.prompt.BusinessPrompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    Optional<BusinessPrompt> findByBusinessNameAndPromptKeyAndActiveTrue(
            String businessName, String promptKey);

    List<BusinessPrompt> findAllByBusinessNameAndActiveTrue(final String businessName);

    Page<BusinessPrompt> findAll(Pageable pageable);

    Optional<BusinessPrompt> findById(Long id);

    @Modifying
    @Query("DELETE FROM BusinessPrompt bp WHERE bp.id IN :ids")
    void deleteAllByIdIn(@Param("ids") List<Long> ids);

    List<BusinessPrompt> findAllByIdIn(List<Long> ids);
    Optional<BusinessPrompt> findByBusinessNameAndPromptKey(String businessName, String promptKey);

    Long countByBusinessName(String businessName);

    Long countByBusinessNameAndActive(String businessName, boolean isActive);
}
