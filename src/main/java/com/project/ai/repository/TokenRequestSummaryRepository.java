package com.project.ai.repository;

import com.project.ai.model.TokenRequestSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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


    @Query("SELECT COUNT(t) FROM TokenRequestSummary t WHERE t.createdAt >= :startOfDay")
    Long countRequestsToday(@Param("startOfDay") LocalDateTime startOfDay);

    Long countBy();

    @Query("SELECT COUNT(DISTINCT t.userId) FROM TokenRequestSummary t WHERE t.createdAt >= :startOfDay")
    Long countActiveUsersToday(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COALESCE(SUM(t.totalTokens), 0) FROM TokenRequestSummary t WHERE t.createdAt >= :startOfDay")
    Long sumTokensToday(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COALESCE(SUM(t.totalTokens), 0) FROM TokenRequestSummary t")
    Long sumTokensTotal();

    @Query("SELECT COALESCE(AVG(t.totalDurationMs), 0) FROM TokenRequestSummary t")
    Double avgResponseTimeMs();

    @Query("SELECT t.userMessage, COUNT(t) FROM TokenRequestSummary t GROUP BY t.userMessage")
    List<Object[]> countByType();

    @Query("SELECT CAST(t.createdAt AS date), AVG(t.totalDurationMs) " +
            "FROM TokenRequestSummary t " +
            "WHERE t.createdAt >= :from " +
            "GROUP BY CAST(t.createdAt AS date) " +
            "ORDER BY CAST(t.createdAt AS date) ASC")
    List<Object[]> findResponseTimeTrend(@Param("from") LocalDateTime from);

    @Query("SELECT t FROM TokenRequestSummary t ORDER BY t.createdAt DESC")
    List<TokenRequestSummary> findRecentRequests(Pageable pageable);
}
