package com.blamex321.document_service.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.blamex321.document_service.dto.AIRequest;
import com.blamex321.document_service.dto.AIResponse;
import com.blamex321.document_service.model.Document;
import com.blamex321.document_service.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final TextExtractionService textExtractionService;
    private final String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
    private final RestTemplate restTemplate;

    public Document upload(MultipartFile file, String userEmail) throws IOException {

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File dest = new File(directory, fileName);
        file.transferTo(dest);

        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .filePath(fileName)
                .uploadedBy(userEmail)
                .uploadedAt(LocalDateTime.now())
                .processingStatus("PROCESSING")
                .build();

        Document saved = documentRepository.save(document);

        // 🔥 Trigger async processing
        processDocument(saved.getId());

        return saved;
    }
    
    public List<Document> findByUserEmail(String email) {
		return documentRepository.findByUploadedBy(email);
	}
    
    @Async
    public void processDocument(String documentId) {

        Document doc = documentRepository.findById(documentId)
                .orElseThrow();

        try {
            String fullPath = uploadDir + File.separator + doc.getFilePath();
            File file = new File(fullPath);

            // 1️⃣ Extract text
            String extractedText = textExtractionService.extractText(file);
            doc.setExtractedText(extractedText);

            // 2️⃣ Call AI service
            AIRequest request = new AIRequest();
            request.setText(extractedText);

            AIResponse response = restTemplate.postForObject(
                    "http://localhost:8083/ai/analyze",
                    request,
                    AIResponse.class
            );

            // 3️⃣ Save AI results
            doc.setSummary(response.getSummary());
            doc.setClassification(response.getClassification());
            doc.setRiskScore(response.getRiskScore());
            doc.setProcessingStatus("COMPLETED");

            documentRepository.save(doc);

        } catch (Exception e) {
            doc.setProcessingStatus("FAILED");
            e.printStackTrace();
            documentRepository.save(doc);
        }
    }
}
