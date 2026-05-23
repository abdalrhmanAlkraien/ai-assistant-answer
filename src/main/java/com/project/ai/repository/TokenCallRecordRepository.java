package com.project.ai.repository;

import com.project.ai.model.TokenCallRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 1:32 AM
 */
@Repository
public interface TokenCallRecordRepository extends JpaRepository<TokenCallRecord, Long> {
    List<TokenCallRecord> findByRequestSummaryRequestId(String requestId);

    @Query("""
            SELECT c.callName,
                   SUM(c.inputTokens)  AS totalInput,
                   SUM(c.outputTokens) AS totalOutput,
                   SUM(c.totalTokens)  AS grandTotal,
                   AVG(c.durationMs)   AS avgDuration,
                   COUNT(c)            AS callCount
            FROM TokenCallRecord c
            GROUP BY c.callName
            ORDER BY grandTotal DESC
            """)
    List<Object[]> aggregateByCallName();

    @Query("""
            SELECT c.callName,
                   SUM(c.inputTokens)  AS totalInput,
                   SUM(c.outputTokens) AS totalOutput,
                   SUM(c.totalTokens)  AS grandTotal,
                   AVG(c.durationMs)   AS avgDuration,
                   COUNT(c)            AS callCount
            FROM TokenCallRecord c
            WHERE c.requestSummary.userId = :userId
            GROUP BY c.callName
            ORDER BY grandTotal DESC
            """)
    List<Object[]> aggregateByCallNameForUser(@Param("userId") String userId);
}
