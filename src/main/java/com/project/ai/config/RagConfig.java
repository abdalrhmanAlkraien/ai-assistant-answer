package com.project.ai.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.regions.Region;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 05/05/2026
 * @Time: 9:01 PM
 */
@Configuration
@RequiredArgsConstructor
@Log4j2
public class RagConfig {

    private final LangChain4jProperties properties;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    EmbeddingModel embeddingModel() {
        log.info("Creating HuggingFace EmbeddingModel --> model id: {}",
                properties.getEmbeddings().getHuggingFace().getModelId());
        return HuggingFaceEmbeddingModel.builder()
                .accessToken(properties.getEmbeddings().getHuggingFace().getAccessToken())
                .modelId(properties.getEmbeddings().getHuggingFace().getModelId())
                .waitForModel(true)
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    // ── Keep existing ollama/nvidia beans ─────────────────────────────────────

    @Bean("englishChatModel")
    @Primary
    public ChatModel englishChatModel() {
        return buildModel("english");
    }

    @Bean("arabicChatModel")
    public ChatModel arabicChatModel() {
        return buildModel("arabic");
    }

    @Bean
    public ChatModel chatModel() {
        return buildModel("powerful");
    }

    // ── New planner model beans ───────────────────────────────────────────────

    @Bean("fastChatModel")
    public ChatModel fastChatModel() {
        return buildModel("fast");
    }

    @Bean("mediumChatModel")
    public ChatModel mediumChatModel() {
        return buildModel("medium");
    }

    @Bean("powerfulChatModel")
    public ChatModel powerfulChatModel() {
        return buildModel("powerful");
    }

    @Bean
    public ApacheTikaDocumentParser tikaParser() {
        return new ApacheTikaDocumentParser();
    }

    @Bean
    @Lazy
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Creating Chroma EmbeddingStore (Chroma 0.5+ compatible) --> {} | collection: {}",
                properties.getVectorStore().getChroma().getBaseUrl(),
                properties.getVectorStore().getChroma().getCollectionName());

        return ChromaEmbeddingStore.builder()
                .baseUrl(properties.getVectorStore().getChroma().getBaseUrl())
                .collectionName(properties.getVectorStore().getChroma().getCollectionName())
                .apiVersion(ChromaApiVersion.V1)

                .build();
    }

    public Supplier<EmbeddingStore<TextSegment>> embeddingStoreSupplier() {
        return () -> {
            log.info("Creating fresh ChromaEmbeddingStore instance for collection: {}",
                    properties.getVectorStore().getChroma().getCollectionName());
            return ChromaEmbeddingStore.builder()
                    .baseUrl(properties.getVectorStore().getChroma().getBaseUrl())
                    .collectionName(properties.getVectorStore().getChroma().getCollectionName())
                    .apiVersion(ChromaApiVersion.V1)
                    .build();
        };
    }

    @Bean
    public DocumentSplitter documentSplitter() {
        log.info("Creating DocumentSplitter --> chunkSize: {}, overlap: {}",
                properties.getDocumentParser().getApachePdfbox().getChunkSize(),
                properties.getDocumentParser().getApachePdfbox().getChunkOverlap());

        return DocumentSplitters.recursive(properties.getDocumentParser().getApachePdfbox().getChunkSize(),
                properties.getDocumentParser().getApachePdfbox().getChunkOverlap());
    }

    @Bean
    public ApachePdfBoxDocumentParser pdfParser() {
        log.info("Creating DocumentParser --> pdfParser: ApachePdfBoxDocumentParser");
        return new ApachePdfBoxDocumentParser();
    }


    public ChatModel buildModel(String modelKey) {
        LangChain4jProperties.ChatModel chatConfig      = properties.getChatModel();
        LangChain4jProperties.ChatModel.ModelConfig modelConfig = chatConfig.getModels().get(modelKey);

        if (modelConfig == null) {
            throw new IllegalArgumentException("Model config not found for key: " + modelKey);
        }

        LangChain4jProperties.ChatModel.ProviderConfig providerConfig =
                chatConfig.getProviders().get(modelConfig.getProvider());

        if (providerConfig == null) {
            throw new IllegalArgumentException("Provider not found: " + modelConfig.getProvider());
        }

        log.info("[RagConfig] Building model key={} name={} provider={}",
                modelKey, modelConfig.getModelName(), modelConfig.getProvider());

        return switch (modelConfig.getProvider()) {
            case "nvidia", "openai" -> OpenAiChatModel.builder()
                    .baseUrl(providerConfig.getBaseUrl())
                    .apiKey(providerConfig.getApiKey())
                    .modelName(modelConfig.getModelName())
                    .temperature(chatConfig.getTemperature())
                    .maxCompletionTokens(modelConfig.getMaxTokens())
                    .timeout(chatConfig.getTimeout())
                    .build();

            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(providerConfig.getBaseUrl())
                    .modelName(modelConfig.getModelName())
                    .temperature(chatConfig.getTemperature())
                    .timeout(chatConfig.getTimeout())
                    .build();

            case "bedrock" -> BedrockChatModel.builder()
                    .region(Region.of(providerConfig.getRegion()))
                    .modelId(modelConfig.getModelName())
                    .build();

            default -> throw new IllegalArgumentException("Unknown provider: " + modelConfig.getProvider());
        };
    }
}
