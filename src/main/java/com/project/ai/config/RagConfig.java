package com.project.ai.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
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

    @Bean
    public ChatModel chatModel() {
        log.info("Creating NVIDIA ChatModel --> {} | {}",
                properties.getChatModel().getNvidia().getBaseUrl(),
                properties.getChatModel().getNvidia().getModelName());

        return OpenAiChatModel.builder()
                .baseUrl(properties.getChatModel().getNvidia().getBaseUrl())
                .apiKey(properties.getChatModel().getNvidia().getApiKey())
                .modelName(properties.getChatModel().getNvidia().getModelName())
                .temperature(properties.getChatModel().getNvidia().getTemperature())
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    @Bean("englishChatModel")
    @Primary
    public ChatModel englishChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(properties.getChatModel().getNvidia().getBaseUrl())
                .apiKey(properties.getChatModel().getNvidia().getApiKey())
                .modelName(properties.getChatModel().getNvidia().getEnglishModelName())
                .temperature(properties.getChatModel().getNvidia().getTemperature())
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    @Bean("arabicChatModel")
    public ChatModel arabicChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(properties.getChatModel().getNvidia().getBaseUrl())
                .apiKey(properties.getChatModel().getNvidia().getApiKey())
                .modelName(properties.getChatModel().getNvidia().getArabicModelName())
                .temperature(properties.getChatModel().getNvidia().getTemperature())
                .timeout(Duration.ofMinutes(2))
                .build();
    }

//    @Bean
//    public ChatModel chatModel() {
//        log.info("Creating Ollama ChatModel --> {} | {}",
//                properties.getChatModel().getOllama().getBaseUrl(),
//                properties.getChatModel().getOllama().getModelName());
//
//        return OllamaChatModel.builder()
//                .baseUrl(properties.getChatModel().getOllama().getBaseUrl())
//                .modelName(properties.getChatModel().getOllama().getModelName())
//                .temperature(0.7)
//                .timeout(Duration.ofMinutes(2))
//                .build();
//    }
//
//    @Bean("englishChatModel")
//    @Primary
//    public ChatModel englishChatModel(LangChain4jProperties properties) {
//        return OllamaChatModel.builder()
//                .baseUrl(properties.getChatModel().getOllama().getBaseUrl())
//                .modelName(properties.getChatModel().getOllama().getEnglishModelName())
//                .temperature(properties.getChatModel().getOllama().getTemperature())
//                .timeout(Duration.ofMinutes(2))
//                .build();
//    }
//
//    @Bean("arabicChatModel")
//    public ChatModel arabicChatModel(LangChain4jProperties properties) {
//        return OllamaChatModel.builder()
//                .baseUrl(properties.getChatModel().getOllama().getBaseUrl())
//                .modelName(properties.getChatModel().getOllama().getArabicModelName())
//                .temperature(properties.getChatModel().getOllama().getTemperature())
//                .timeout(Duration.ofMinutes(2))
//                .build();
//    }

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
}
