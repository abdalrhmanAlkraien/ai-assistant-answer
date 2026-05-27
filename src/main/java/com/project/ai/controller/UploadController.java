package com.project.ai.controller;

import com.project.ai.dto.IngestionResponse;
import com.project.ai.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/05/2026
 * @Time: 10:37 PM
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "Document and file upload for vector DB ingestion")
public class UploadController {

    private final UploadService uploadService;


    @Operation(
            summary = "Upload a document",
            description = "Upload a PDF or text file to be parsed, chunked, embedded, and indexed into ChromaDB for semantic search"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File uploaded and indexed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or unsupported file type", content = @Content),
            @ApiResponse(responseCode = "500", description = "Ingestion failed", content = @Content)
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Deprecated(since = "already we have index value inside the product controller")
    public ResponseEntity<IngestionResponse> uploadDocument(
            @Parameter(description = "PDF or text file to upload", required = true)
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(uploadService.uploadFile(file));
    }
}
