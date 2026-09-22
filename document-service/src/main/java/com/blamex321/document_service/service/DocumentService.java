package com.blamex321.document_service.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.blamex321.document_service.dto.AIChatRequest;
import com.blamex321.document_service.dto.AIChatResponse;
import com.blamex321.document_service.dto.DocumentAnalysisResponse;
import com.blamex321.document_service.dto.DocumentChatResponse;
import com.blamex321.document_service.dto.DocumentResponse;
import com.blamex321.document_service.dto.PagedResponse;
import com.blamex321.document_service.exception.DocumentNotFoundException;
import com.blamex321.document_service.exception.InvalidFileException;
import com.blamex321.document_service.exception.UnauthorizedAccessException;
import com.blamex321.document_service.model.Document;
import com.blamex321.document_service.repository.DocumentRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final AsyncDocumentProcessor asyncDocumentProcessor;
    private final RestTemplate restTemplate;

    @Value("${storage.upload-dir:}")
    private String configuredUploadDir;

    @Value("${ai.chat.url:http://localhost:8083/ai/chat}")
    private String aiChatUrl;

    private String uploadDir;

    @PostConstruct
    public void init() {
        if (configuredUploadDir != null && !configuredUploadDir.isBlank()) {
            this.uploadDir = configuredUploadDir;
        } else {
            this.uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
        }
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public DocumentResponse upload(MultipartFile file, String userEmail) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Cannot upload an empty file");
        }

        // Sanitize filename to prevent Path Traversal
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.bin");
        if (originalFilename.contains("..")) {
            throw new InvalidFileException("Filename contains invalid relative path sequence: " + originalFilename);
        }

        String storedFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + originalFilename;
        File destination = new File(uploadDir, storedFileName);
        file.transferTo(destination);

        Document document = Document.builder()
                .fileName(originalFilename)
                .fileType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileSize(file.getSize())
                .filePath(storedFileName)
                .uploadedBy(userEmail)
                .uploadedAt(LocalDateTime.now())
                .processingStatus("PROCESSING")
                .keywords(Collections.emptyList())
                .build();

        Document saved = documentRepository.save(document);
        log.info("Document metadata persisted with ID: {} for user: {}", saved.getId(), userEmail);

        // Dispatch asynchronous processing via separate Spring bean (ensures @Async proxy is active)
        asyncDocumentProcessor.processDocumentAsync(saved.getId(), uploadDir);

        return mapToResponse(saved);
    }

    public PagedResponse<DocumentResponse> findByUserEmail(String email, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Document> docPage = documentRepository.findByUploadedBy(email, pageRequest);
        List<DocumentResponse> responseList = docPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<DocumentResponse>builder()
                .content(responseList)
                .pageNumber(docPage.getNumber())
                .pageSize(docPage.getSize())
                .totalElements(docPage.getTotalElements())
                .totalPages(docPage.getTotalPages())
                .isLast(docPage.isLast())
                .build();
    }

    public List<DocumentResponse> findAllByUserEmail(String email) {
        return documentRepository.findByUploadedBy(email).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DocumentResponse getById(String id, String userEmail) {
        Document doc = getDocumentAndVerifyOwnership(id, userEmail);
        return mapToResponse(doc);
    }

    public DocumentAnalysisResponse getAnalysis(String id, String userEmail) {
        Document doc = getDocumentAndVerifyOwnership(id, userEmail);
        return DocumentAnalysisResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .status(doc.getProcessingStatus())
                .summary(doc.getSummary())
                .classification(doc.getClassification())
                .riskScore(doc.getRiskScore())
                .keywords(doc.getKeywords() != null ? doc.getKeywords() : Collections.emptyList())
                .build();
    }

    public DocumentChatResponse chatWithDocument(String id, String userEmail, String question) {
        Document doc = getDocumentAndVerifyOwnership(id, userEmail);

        if (!"COMPLETED".equalsIgnoreCase(doc.getProcessingStatus())) {
            throw new InvalidFileException("Document processing is still in progress (Status: " + doc.getProcessingStatus() + "). Please wait until processing completes.");
        }

        if (doc.getExtractedText() == null || doc.getExtractedText().isBlank()) {
            throw new InvalidFileException("Document contains no extracted text for Q&A.");
        }

        log.info("Sending RAG Q&A query to AI service for document ID: {}", id);
        AIChatRequest chatRequest = new AIChatRequest(doc.getExtractedText(), question);

        AIChatResponse chatResponse = null;
        try {
            chatResponse = restTemplate.postForObject(aiChatUrl, chatRequest, AIChatResponse.class);
        } catch (Exception e) {
            log.error("Failed to call AI Chat service at {}: {}", aiChatUrl, e.getMessage(), e);
        }

        String answer = (chatResponse != null && chatResponse.getAnswer() != null)
                ? chatResponse.getAnswer()
                : "Unable to retrieve an answer at this time. Please verify that the AI service is operational.";

        List<String> sources = (chatResponse != null && chatResponse.getRelevantSources() != null)
                ? chatResponse.getRelevantSources()
                : Collections.emptyList();

        String responseModel = (chatResponse != null && chatResponse.getModel() != null)
                ? chatResponse.getModel()
                : "local-heuristic-engine";

        return DocumentChatResponse.builder()
                .documentId(doc.getId())
                .fileName(doc.getFileName())
                .question(question)
                .answer(answer)
                .relevantSources(sources)
                .model(responseModel)
                .build();
    }

    public File getFileForDownload(String id, String userEmail) {
        Document doc = getDocumentAndVerifyOwnership(id, userEmail);
        File file = new File(uploadDir, doc.getFilePath());
        if (!file.exists()) {
            throw new DocumentNotFoundException("Document file not found on disk: " + doc.getFileName());
        }
        return file;
    }

    public Document getDocumentEntity(String id, String userEmail) {
        return getDocumentAndVerifyOwnership(id, userEmail);
    }

    public void deleteDocument(String id, String userEmail) {
        Document doc = getDocumentAndVerifyOwnership(id, userEmail);

        // Delete physical file from disk
        try {
            Path path = Paths.get(uploadDir, doc.getFilePath());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete physical file {}: {}", doc.getFilePath(), e.getMessage());
        }

        documentRepository.delete(doc);
        log.info("Document ID {} successfully deleted by user {}", id, userEmail);
    }

    private Document getDocumentAndVerifyOwnership(String id, String userEmail) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + id));

        if (!doc.getUploadedBy().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedAccessException("You do not have permission to access document ID: " + id);
        }

        return doc;
    }

    public DocumentResponse mapToResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .uploadedBy(doc.getUploadedBy())
                .uploadedAt(doc.getUploadedAt())
                .processingStatus(doc.getProcessingStatus())
                .summary(doc.getSummary())
                .classification(doc.getClassification())
                .riskScore(doc.getRiskScore())
                .keywords(doc.getKeywords() != null ? doc.getKeywords() : Collections.emptyList())
                .build();
    }
}
