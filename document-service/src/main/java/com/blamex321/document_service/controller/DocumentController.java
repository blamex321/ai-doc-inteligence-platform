package com.blamex321.document_service.controller;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.blamex321.document_service.dto.DocumentAnalysisResponse;
import com.blamex321.document_service.dto.DocumentChatRequest;
import com.blamex321.document_service.dto.DocumentChatResponse;
import com.blamex321.document_service.dto.DocumentResponse;
import com.blamex321.document_service.dto.PagedResponse;
import com.blamex321.document_service.model.Document;
import com.blamex321.document_service.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @RequestHeader("X-User-Email") String email,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return documentService.upload(file, email);
    }

    @GetMapping("/my")
    public PagedResponse<DocumentResponse> getMyDocuments(
            @RequestHeader("X-User-Email") String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return documentService.findByUserEmail(email, page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public DocumentResponse getById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email
    ) {
        return documentService.getById(id, email);
    }

    @GetMapping("/{id}/analysis")
    public DocumentAnalysisResponse getAnalysis(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email
    ) {
        return documentService.getAnalysis(id, email);
    }

    @PostMapping("/{id}/chat")
    public DocumentChatResponse chatWithDocument(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestBody DocumentChatRequest request
    ) {
        return documentService.chatWithDocument(id, email, request.getQuestion());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email
    ) {
        Document doc = documentService.getDocumentEntity(id, email);
        File file = documentService.getFileForDownload(id, email);

        Resource resource = new FileSystemResource(file);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(doc.getFileType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.length())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email
    ) {
        documentService.deleteDocument(id, email);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully", "id", id));
    }
}
