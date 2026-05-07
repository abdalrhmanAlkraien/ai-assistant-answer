package com.project.ai.controller;

import com.project.ai.dto.IngestionResponse;
import com.project.ai.service.UploadService;
import dev.langchain4j.store.embedding.IngestionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/upload")
    public ResponseEntity<IngestionResponse> uploadDocument(@RequestPart("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(uploadService.uploadFile(file));
    }
}
