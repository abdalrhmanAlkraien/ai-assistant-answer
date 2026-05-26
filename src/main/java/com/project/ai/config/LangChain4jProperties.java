package com.project.ai.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 05/05/2026
 * @Time: 9:07 PM
 */
@ConfigurationProperties(prefix = "langchain4j")
@Validated
@Data
public class LangChain4jProperties {


    private Embeddings embeddings;
    private VectorStore vectorStore;
    private ChatModel chatModel;
    private DocumentParser documentParser;

    // ─── Embeddings ───────────────────────────────────────────
    @Data
    public static class Embeddings {
        private HuggingFace huggingFace;

        @Data
        public static class HuggingFace {
            @NotBlank
            private String modelId;
            private String accessKey;
            private String accessToken;
            private String apiKey;
            private boolean waitForModel = true;
            private Duration timeout = Duration.ofMinutes(3);
        }
    }

    // ─── Vector Store ─────────────────────────────────────────
    @Data
    public static class VectorStore {
        private Chroma chroma;
        private int maxFileSizeMbytes = 50;

        @Data
        public static class Chroma {
            @NotBlank
            private String baseUrl;
            private String url;
            @NotBlank
            private String collectionName;
            private int topKMax = 20;
            private double defaultMinScoreThreshold = 0.62;
        }
    }

    // ─── Chat Model ───────────────────────────────────────────
    @Data
    public static class ChatModel {
        private Ollama ollama;                          // keep existing
        private Double temperature = 0.7;
        private Duration timeout = Duration.ofSeconds(180);
        private Map<String, ProviderConfig> providers = new HashMap<>();
        private Map<String, ModelConfig> models = new HashMap<>();

        // ── keep existing for backward compatibility ──────────
        @Data
        public static class Ollama {
            private String baseUrl;
            private String arabicModelName;
            private String englishModelName;
            private String modelName;
            private String apiKey;
            private double temperature = 0.7;
            private Duration timeout = Duration.ofSeconds(60);
        }

        // ── new provider config ───────────────────────────────
        @Data
        public static class ProviderConfig {
            private String baseUrl;
            private String apiKey;
            private String region;      // bedrock only
            private String accessKey;   // bedrock only
            private String secretKey;   // bedrock only
        }

        // ── new model config ──────────────────────────────────
        @Data
        public static class ModelConfig {
            private String modelName;
            private String provider;    // nvidia | ollama | bedrock
            private Integer maxTokens;
        }
    }

    // ─── Document Parser ──────────────────────────────────────
    @Data
    public static class DocumentParser {
        private ApachePdfbox apachePdfbox;
        private ApacheTika apacheTika;

        @Data
        public static class ApachePdfbox {
            private int chunkSize = 1000;
            private int chunkOverlap = 200;
        }

        @Data
        public static class ApacheTika {
            private boolean enabled = true;
        }
    }
}
