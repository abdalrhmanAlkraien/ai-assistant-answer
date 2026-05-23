package com.project.ai.dto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 1:35 AM
 */
@Slf4j
public class TokenTracker {

    @Getter private final String requestId;
    @Getter
    private final String userId;
    @Getter private final String modelName;
    @Getter private final String userMessage;


    private final Instant startTime = Instant.now();
    private final List<CallEntry> entries = new ArrayList<>();

    public TokenTracker(String requestId, String userId, String modelName, String userMessage) {
        this.requestId   = requestId;
        this.userId      = userId;
        this.modelName   = modelName;
        this.userMessage = userMessage;
    }

    /**
     * Wrap any LLM call.
     * tokenExtractor returns [inputTokens, outputTokens].
     *
     * Usage:
     *   Response<AiMessage> response = tracker.track(
     *       "intent-analysis",
     *       () -> chatModel.generate(messages),
     *       r  -> new int[]{ r.tokenUsage().inputTokenCount(),
     *                        r.tokenUsage().outputTokenCount() }
     *   );
     */
    public <T> T track(String callName, Supplier<T> llmCall, Function<T, int[]> tokenExtractor) {
        long start    = System.currentTimeMillis();
        T result      = llmCall.get();
        long duration = System.currentTimeMillis() - start;

        int[] tokens = tokenExtractor.apply(result);
        int input    = tokens[0];
        int output   = tokens[1];

        entries.add(new CallEntry(callName, input, output, duration, modelName, LocalDateTime.now()));

        log.info("[Token] [{}] call={} input={} output={} total={} duration={}ms",
                requestId, callName, input, output, input + output, duration);

        return result;
    }

    /**
     * Use this when you already have token counts (e.g. from streaming).
     */
    public void record(String callName, String modelName, int inputTokens, int outputTokens, long durationMs) {
        entries.add(new CallEntry(callName, inputTokens, outputTokens, durationMs, modelName, LocalDateTime.now()));

        log.info("[Token] [{}] call={} input={} output={} total={} duration={}ms",
                requestId, callName, inputTokens, outputTokens, inputTokens + outputTokens, durationMs);
    }

    public List<CallEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public long totalDurationMs() {
        return Instant.now().toEpochMilli() - startTime.toEpochMilli();
    }

    public record CallEntry(
            String callName,
            int inputTokens,
            int outputTokens,
            long durationMs,
            String modelName,
            LocalDateTime calledAt
    ) {
        public int totalTokens() { return inputTokens + outputTokens; }
    }
}
