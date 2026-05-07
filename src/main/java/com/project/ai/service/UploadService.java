package com.project.ai.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.IngestionResponse;
import com.project.ai.dto.Product;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/05/2026
 * @Time: 10:37 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class UploadService {


    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final LangChain4jProperties properties;
    private final ObjectMapper mapper;

    public IngestionResponse uploadFile(final MultipartFile file) throws IOException {

        // create the file here
        Path temp = Files.createTempFile("upload-", "-" + file.getOriginalFilename());
        file.transferTo(temp);

        List<Product> products = parseFile(file);

        List<TextSegment> textSegments = fetchSegments(products);

        List<Embedding> embeddings = embeddingModel
                .embedAll(textSegments)
                .content();

        embeddingStore.addAll(embeddings, textSegments);

        return IngestionResponse.builder()
                .fileName(file.getOriginalFilename())
                .size(textSegments.size())   // 50 products = 50 chunks
                .totalTokensUsed(0)
                .status("SUCCESS")
                .ingestedAt(LocalDateTime.now())
                .build();
    }

    private List<Product> parseFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".json")) {
            throw new IllegalArgumentException("Only JSON files are supported, received: " + filename);
        }

        long maxBytes = 50L * 1024 * 1024; // 50MB
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size exceeds 50MB limit: " + file.getSize());
        }
        try {

            List<Product> products = mapper.readValue(
                    file.getInputStream(),
                    new TypeReference<List<Product>>() {}
            );

            // 5. Validate parsed result
            if (products == null || products.isEmpty()) {
                throw new IllegalArgumentException("JSON file contains no products");
            }

            // 6. Validate each product
            products.forEach(this::validateProduct);

            return products;

        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getOriginalMessage());
        } catch (JsonMappingException e) {
            throw new IllegalArgumentException("JSON structure does not match Product schema: " + e.getOriginalMessage());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
    }

    private void validateProduct(Product product) {
        if (product.getId() == null || product.getId().isBlank()) {
            throw new IllegalArgumentException("Product missing id field");
        }
        if (product.getContent() == null || product.getContent().isBlank()) {
            throw new IllegalArgumentException("Product [" + product.getId() + "] has empty content");
        }
    }

    private List<TextSegment> fetchSegments(List<Product> products) {

        return products.stream()
                .map(map-> TextSegment.from(
                        map.getContent(),
                        Metadata.metadata("id", map.getId())
                )).collect(Collectors.toList());
    }

    @Deprecated
    private Document loadDocument(Path path) {
        String fileName = path.toString().toLowerCase();

        Document document;
        if (fileName.endsWith(".pdf")) {
            document = FileSystemDocumentLoader.loadDocument(path, new ApachePdfBoxDocumentParser());
        } else if (fileName.endsWith(".docx")) {
            document = FileSystemDocumentLoader.loadDocument(path, new ApachePoiDocumentParser());
        } else {
            document = FileSystemDocumentLoader.loadDocument(path, new TextDocumentParser());
        }

        document.metadata().put("source", fileName);
        document.metadata().put("ingested_at", Instant.now().toString());
        return document;
    }

    private EmbeddingStoreIngestor buildIngestor() {
        var pdfConfig = properties.getDocumentParser().getApachePdfbox();

        return EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(
                        DocumentSplitters.recursive(
                                pdfConfig.getChunkSize(),    // from yml: 1000
                                pdfConfig.getChunkOverlap()  // from yml: 200
                        )
                )
                .build();
    }
}
