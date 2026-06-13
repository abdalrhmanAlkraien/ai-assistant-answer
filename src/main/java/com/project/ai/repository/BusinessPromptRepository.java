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

    List<BusinessPrompt> findAllByBusinessNameAndActiveTrue(String businessName);

    Page<BusinessPrompt> findAll(Pageable pageable);

    Optional<BusinessPrompt> findById(Long id);

    Optional<BusinessPrompt> findByBusinessNameAndPromptKey(String businessName, String promptKey);

    Long countByBusinessName(String businessName);
    Long countByBusinessNameAndActive(String businessName, boolean isActive);

    List<BusinessPrompt> findAllByIdIn(List<Long> ids);

    @Modifying
    @Query("DELETE FROM BusinessPrompt bp WHERE bp.id IN :ids")
    void deleteAllByIdIn(@Param("ids") List<Long> ids);

    // ── Versioning ────────────────────────────────────────────────────────────

    // all versions for a prompt key ordered newest first
    List<BusinessPrompt> findAllByBusinessNameAndPromptKeyOrderByVersionDesc(
            String businessName, String promptKey);

    // latest version number for a prompt key
    @Query("SELECT COALESCE(MAX(bp.version), 0) FROM BusinessPrompt bp " +
            "WHERE bp.businessName = :businessName AND bp.promptKey = :promptKey")
    Integer findMaxVersionByBusinessNameAndPromptKey(
            @Param("businessName") String businessName,
            @Param("promptKey") String promptKey);

    // find specific version
    Optional<BusinessPrompt> findByBusinessNameAndPromptKeyAndVersion(
            String businessName, String promptKey, Integer version);

    // find previous active version for rollback
    @Query("SELECT bp FROM BusinessPrompt bp " +
            "WHERE bp.businessName = :businessName " +
            "AND bp.promptKey = :promptKey " +
            "AND bp.active = false " +
            "AND bp.version = (SELECT MAX(bp2.version) FROM BusinessPrompt bp2 " +
            "                  WHERE bp2.businessName = :businessName " +
            "                  AND bp2.promptKey = :promptKey " +
            "                  AND bp2.active = false)")
    Optional<BusinessPrompt> findPreviousVersion(
            @Param("businessName") String businessName,
            @Param("promptKey") String promptKey);

    // check if active version exists
    boolean existsByBusinessNameAndPromptKeyAndActiveTrue(
            String businessName, String promptKey);

    @Query("SELECT bp FROM BusinessPrompt bp " +
            "WHERE bp.businessName = :businessName " +
            "AND (:promptKey IS NULL OR bp.promptKey = :promptKey) " +
            "AND (:active IS NULL OR bp.active = :active)")
    Page<BusinessPrompt> findAllWithFilters(
            @Param("businessName") String businessName,
            @Param("promptKey")    String promptKey,
            @Param("active")       Boolean active,
            Pageable pageable);
}
