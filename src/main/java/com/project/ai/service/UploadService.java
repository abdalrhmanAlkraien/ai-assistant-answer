package com.project.ai.service;

import com.project.ai.config.LangChain4jProperties;
import com.project.ai.config.RagConfig;
import dev.langchain4j.data.document.Document;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

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

    public IngestionResult uploadFile(final MultipartFile file) throws IOException {

        // create the file here
        Path temp = Files.createTempFile("upload-", "-" + file.getOriginalFilename());
        file.transferTo(temp);

        // load the document using langChain
        Document document = loadDocument(temp);
        EmbeddingStoreIngestor ingestor = buildIngestor();

        IngestionResult result = ingestor.ingest(document);
        // chunk the file into split

        Files.deleteIfExists(temp);

        return result;
    }

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
