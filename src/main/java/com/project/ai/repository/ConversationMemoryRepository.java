package com.project.ai.repository;

import com.project.ai.model.ConversationMemory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 11/05/2026
 * @Time: 9:42 PM
 */
@Repository
public interface ConversationMemoryRepository extends JpaRepository<ConversationMemory, Long> {

    @Query(value = """
            SELECT * FROM conversation_memory
            WHERE user_id = :userId
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ConversationMemory> findRecentMessages(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    // ─── Similar messages using cosine similarity ─────────────
    @Query(value = """
            SELECT *
            FROM conversation_memory
            WHERE user_id = :userId
            ORDER BY message_vector <=> CAST(:vector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<ConversationMemory> findSimilarMessages(
            @Param("userId") Long userId,
            @Param("vector") String vector,
            @Param("limit") int limit
    );

    // ─── Similar messages with score threshold ────────────────
    @Query(value = """
            SELECT *,
                   1 - (message_vector <=> CAST(:vector AS vector)) AS similarity_score
            FROM conversation_memory
            WHERE user_id = :userId
              AND 1 - (message_vector <=> CAST(:vector AS vector)) > :minScore
            ORDER BY message_vector <=> CAST(:vector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<ConversationMemory> findSimilarMessagesWithScore(
            @Param("userId") Long userId,
            @Param("vector") String vector,
            @Param("limit") int limit,
            @Param("minScore") double minScore
    );

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO conversation_memory 
                (user_id, role, message, message_vector, search_type, matched_products, created_at)
            VALUES 
                (:userId, :role, :message, CAST(:messageVector AS vector), :searchType, :matchedProducts, :createdAt)
            """, nativeQuery = true)
    void insertMemory(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("message") String message,
            @Param("messageVector") String messageVector,
            @Param("searchType") String searchType,
            @Param("matchedProducts") String[] matchedProducts,
            @Param("createdAt") LocalDateTime createdAt
    );

    @Query("SELECT cm.userId, COUNT(cm.id) as messageCount, MAX(cm.createdAt) as lastActivity " +
            "FROM ConversationMemory cm GROUP BY cm.userId ORDER BY MAX(cm.createdAt) DESC")
    Page<Object[]> findAllUsersWithStats(Pageable pageable);

    Page<ConversationMemory> findByUserIdOrderByCreatedAtAsc(Long userId, Pageable pageable);


    @Modifying
    @Query("DELETE FROM ConversationMemory cm WHERE cm.userId = :userId AND cm.id IN :ids")
    void deleteByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    @Modifying
    @Query("DELETE FROM ConversationMemory cm WHERE cm.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);

    List<ConversationMemory> findByUserIdAndIdIn(Long userId, List<Long> ids);

    @Query("SELECT COUNT(DISTINCT cm.userId) FROM ConversationMemory cm")
    Long countDistinctUsers();

    Long countBy();
}
