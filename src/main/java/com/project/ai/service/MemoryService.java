package com.project.ai.service;

import com.project.ai.agents.Language;
import com.project.ai.config.AppProperties;
import com.project.ai.dto.SearchIntent;
import com.project.ai.model.ConversationMemory;
import com.project.ai.model.MessageRole;
import com.project.ai.repository.ConversationMemoryRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 11/05/2026
 * @Time: 10:16 PM
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class MemoryService {

    private final ConversationMemoryRepository memoryRepository;
    private final EmbeddingModel embeddingModel;
    private final AppProperties appProperties;

    public String memoryContext(
            final String userId,
            final String userQuery
    ) {

        log.info("[MemoryService] START");

        if (userId == null) {

            log.warn("[MemoryService] User id is null");
            return "";
        }

        String vectorQuery = toVectorString(convertToVector(userQuery));

        List<ConversationMemory> similar = getSimilarConversation(userId, vectorQuery);
        List<ConversationMemory> recent = getRecentConversation(userId);

        Map<Long, ConversationMemory> merged = mergeConversation(similar, recent);

        if (merged.isEmpty()) {
            log.info("[MemoryService] No memory found for user: {}", userId);
            return "";
        }

        // build context
        String context = merged.values().stream()
                .sorted(Comparator.comparing(ConversationMemory::getCreatedAt))
                .map(m -> m.getRole() == MessageRole.USER
                        ? "User: " + m.getMessage()
                        : "Assistant: " + m.getMessage())
                .collect(Collectors.joining("\n"));

        log.info("[MemoryService] Built memory context for user {} with {} messages",
                userId, merged.size());

        log.debug("[MemoryService] the memory context is : {}", context);

        return context;
    }

    private Map<Long, ConversationMemory> mergeConversation(
            final List<ConversationMemory> similarConversation,
            final List<ConversationMemory> recentConversation)  {
        Map<Long, ConversationMemory> merged = new LinkedHashMap<>();

        recentConversation.forEach(m -> merged.put(m.getId(), m));
        similarConversation.forEach(m -> merged.putIfAbsent(m.getId(), m));

        return merged;
    }

    private List<ConversationMemory> getSimilarConversation(
            final String userId,
            final String userQuery) {

        return memoryRepository.findSimilarMessages(
                userId, userQuery, appProperties.getMemory().getSimilar());
    }

    private List<ConversationMemory> getRecentConversation (
            String userId
    ) {

        return memoryRepository.findRecentMessages(userId, appProperties.getMemory().getContext());
    }

    public void saveMemory(final String userId,
                           final SearchIntent searchIntent,
                           final MessageRole role,
                           final String query,
                           final String[] matchIds,
                           final Language language) {

        String vectorQuery = toVectorString(convertToVector(query));

        log.info("[MemoryService] - Conversation memory for {} ", role.name());
        memoryRepository.insertMemory(
                userId,
                role.name(),
                query,
                vectorQuery,
                searchIntent.getSearchType(),
                matchIds,
                LocalDateTime.now(),
                language.name());
    }


    public void saveMemory(final String userId,
                           final String searchType,
                           final MessageRole role,
                           final String query,
                           final String[] matchIds,
                           final Language language) {

        String vectorQuery = toVectorString(convertToVector(query));

        log.info("[MemoryService] - Conversation memory for {} ", role.name());
        memoryRepository.insertMemory(
                userId,
                role.name(),
                query,
                vectorQuery,
                searchType,
                matchIds,
                LocalDateTime.now(),
                language.name());
    }

    private ConversationMemory buildConversationMemory(
            final String userId,
            final SearchIntent searchIntent,
            final MessageRole role,
            final String query,
            final String[] matchIds) {


        return ConversationMemory
                .builder()
                .role(role)
                .message(query)
                .matchedProducts(matchIds)
                .searchType(searchIntent.getSearchType())
                .userId(userId)
                .messageVector(toVectorString(convertToVector(query)))
                .build();
    }

    private float[] convertToVector(String userQuery) {

        return embeddingModel.embed(userQuery).content().vector();

    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
