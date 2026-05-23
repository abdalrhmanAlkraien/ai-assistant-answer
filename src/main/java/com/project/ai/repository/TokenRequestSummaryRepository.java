package com.project.ai.repository;

import com.project.ai.model.TokenRequestSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 1:31 AM
 */
@Repository
public interface TokenRequestSummaryRepository extends JpaRepository<TokenRequestSummary, Long> {

    Page<TokenRequestSummary> findByUserId(String userId, Pageable pageable);

    @Query("""
            SELECT t FROM TokenRequestSummary t
            LEFT JOIN FETCH t.callRecords
            WHERE t.requestId = :requestId
            """)
    Optional<TokenRequestSummary> findByRequestIdWithCalls(@Param("requestId") String requestId);

    @Query("""
            SELECT SUM(t.totalInputTokens)  FROM TokenRequestSummary t WHERE t.userId = :userId
            """)
    Long sumInputTokensByUserId(@Param("userId") String userId);

    @Query("""
            SELECT SUM(t.totalOutputTokens) FROM TokenRequestSummary t WHERE t.userId = :userId
            """)
    Long sumOutputTokensByUserId(@Param("userId") String userId);

    @Query("""
            SELECT SUM(t.totalTokens) FROM TokenRequestSummary t WHERE t.userId = :userId
            """)
    Long sumTotalTokensByUserId(@Param("userId") String userId);

    @Query("SELECT SUM(t.totalInputTokens)  FROM TokenRequestSummary t")
    Long sumAllInputTokens();

    @Query("SELECT SUM(t.totalOutputTokens) FROM TokenRequestSummary t")
    Long sumAllOutputTokens();

    @Query("SELECT SUM(t.totalTokens) FROM TokenRequestSummary t")
    Long sumAllTotalTokens();

    @Query("SELECT COUNT(t) FROM TokenRequestSummary t")
    Long countAllRequests();

    @Query("SELECT COUNT(t) FROM TokenRequestSummary t WHERE t.userId = :userId")
    Long countRequestsByUserId(@Param("userId") String userId);
}
