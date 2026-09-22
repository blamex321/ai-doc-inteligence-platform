package com.blamex321.document_service.service;

import java.io.File;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.blamex321.document_service.dto.AIRequest;
import com.blamex321.document_service.dto.AIResponse;
import com.blamex321.document_service.model.Document;
import com.blamex321.document_service.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncDocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsyncDocumentProcessor.class);

    private final DocumentRepository documentRepository;
    private final TextExtractionService textExtractionService;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8083/ai/analyze}")
    private String aiServiceUrl;

    @Async
    public void processDocumentAsync(String documentId, String uploadDir) {
        log.info("Starting asynchronous document processing for ID: {}", documentId);

        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.error("Document not found with ID: {}", documentId);
            return;
        }

        try {
            String fullPath = uploadDir + File.separator + doc.getFilePath();
            File file = new File(fullPath);

            if (!file.exists()) {
                throw new IllegalStateException("Uploaded file does not exist on disk: " + fullPath);
            }

            // 1. Extract text via Apache Tika
            log.info("Extracting text for document: {}", doc.getFileName());
            String extractedText = textExtractionService.extractText(file);
            doc.setExtractedText(extractedText);

            // 2. Call AI Service for Intelligent Analysis
            log.info("Dispatching text to AI Service at {} for document: {}", aiServiceUrl, doc.getFileName());
            AIRequest aiRequest = new AIRequest(extractedText);

            AIResponse aiResponse = restTemplate.postForObject(
                    aiServiceUrl,
                    aiRequest,
                    AIResponse.class
            );

            // 3. Save AI results into Document entity
            if (aiResponse != null) {
                doc.setSummary(aiResponse.getSummary());
                doc.setClassification(aiResponse.getClassification());
                doc.setRiskScore(aiResponse.getRiskScore());
                doc.setKeywords(aiResponse.getKeywords() != null ? aiResponse.getKeywords() : Collections.emptyList());
                doc.setProcessingStatus("COMPLETED");
                log.info("AI Analysis completed successfully for document ID: {}", documentId);
            } else {
                doc.setProcessingStatus("FAILED");
                doc.setSummary("AI Service returned empty response");
                log.warn("AI Service returned null response for document ID: {}", documentId);
            }

        } catch (Exception e) {
            log.error("Failed to process document ID {}: {}", documentId, e.getMessage(), e);
            doc.setProcessingStatus("FAILED");
            doc.setSummary("Processing failed: " + e.getMessage());
        }

        documentRepository.save(doc);
    }
}
