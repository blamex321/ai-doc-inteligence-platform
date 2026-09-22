package com.blamex321.document_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.blamex321.document_service.dto.AIChatRequest;
import com.blamex321.document_service.dto.AIChatResponse;
import com.blamex321.document_service.dto.DocumentAnalysisResponse;
import com.blamex321.document_service.dto.DocumentChatResponse;
import com.blamex321.document_service.dto.DocumentResponse;
import com.blamex321.document_service.exception.DocumentNotFoundException;
import com.blamex321.document_service.exception.InvalidFileException;
import com.blamex321.document_service.exception.UnauthorizedAccessException;
import com.blamex321.document_service.model.Document;
import com.blamex321.document_service.repository.DocumentRepository;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AsyncDocumentProcessor asyncDocumentProcessor;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DocumentService documentService;

    @TempDir
    File tempDir;

    private Document sampleDoc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "uploadDir", tempDir.getAbsolutePath());
        ReflectionTestUtils.setField(documentService, "aiChatUrl", "http://localhost:8083/ai/chat");

        sampleDoc = Document.builder()
                .id("doc-123")
                .fileName("test.pdf")
                .fileType("application/pdf")
                .fileSize(1024L)
                .filePath("123_test.pdf")
                .uploadedBy("user@example.com")
                .uploadedAt(LocalDateTime.now())
                .processingStatus("COMPLETED")
                .extractedText("This is sample extracted document content for testing.")
                .summary("Test Summary")
                .classification("Resume")
                .riskScore("Low")
                .keywords(List.of("java", "spring"))
                .build();
    }

    @Test
    @DisplayName("Should successfully upload file, persist metadata and dispatch async processing")
    void upload_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "Dummy PDF content".getBytes()
        );

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId("saved-id-999");
            return doc;
        });

        DocumentResponse response = documentService.upload(file, "user@example.com");

        assertNotNull(response);
        assertEquals("resume.pdf", response.getFileName());
        assertEquals("user@example.com", response.getUploadedBy());
        assertEquals("PROCESSING", response.getProcessingStatus());
        verify(asyncDocumentProcessor).processDocumentAsync(eq("saved-id-999"), anyString());
    }

    @Test
    @DisplayName("Should reject empty file upload with InvalidFileException")
    void upload_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
        );

        assertThrows(InvalidFileException.class, () -> documentService.upload(emptyFile, "user@example.com"));
    }

    @Test
    @DisplayName("Should reject filename with path traversal sequence")
    void upload_PathTraversal_ThrowsException() {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file", "../../etc/passwd", "text/plain", "data".getBytes()
        );

        assertThrows(InvalidFileException.class, () -> documentService.upload(maliciousFile, "user@example.com"));
    }

    @Test
    @DisplayName("Should get document by ID when requester is owner")
    void getById_Success() {
        when(documentRepository.findById("doc-123")).thenReturn(Optional.of(sampleDoc));

        DocumentResponse response = documentService.getById("doc-123", "user@example.com");

        assertNotNull(response);
        assertEquals("doc-123", response.getId());
        assertEquals("test.pdf", response.getFileName());
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException when accessing another user's document")
    void getById_Unauthorized_ThrowsException() {
        when(documentRepository.findById("doc-123")).thenReturn(Optional.of(sampleDoc));

        assertThrows(UnauthorizedAccessException.class, () -> documentService.getById("doc-123", "attacker@example.com"));
    }

    @Test
    @DisplayName("Should throw DocumentNotFoundException when document does not exist")
    void getById_NotFound_ThrowsException() {
        when(documentRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentService.getById("non-existent", "user@example.com"));
    }

    @Test
    @DisplayName("Should get document analysis successfully")
    void getAnalysis_Success() {
        when(documentRepository.findById("doc-123")).thenReturn(Optional.of(sampleDoc));

        DocumentAnalysisResponse analysis = documentService.getAnalysis("doc-123", "user@example.com");

        assertNotNull(analysis);
        assertEquals("Test Summary", analysis.getSummary());
        assertEquals("Resume", analysis.getClassification());
        assertEquals("Low", analysis.getRiskScore());
    }

    @Test
    @DisplayName("Should successfully execute RAG Q&A with document")
    void chatWithDocument_Success() {
        when(documentRepository.findById("doc-123")).thenReturn(Optional.of(sampleDoc));
        AIChatResponse mockAiResp = AIChatResponse.builder()
                .answer("This is the grounded answer.")
                .relevantSources(List.of("Source excerpt 1"))
                .build();

        when(restTemplate.postForObject(anyString(), any(AIChatRequest.class), eq(AIChatResponse.class)))
                .thenReturn(mockAiResp);

        DocumentChatResponse response = documentService.chatWithDocument("doc-123", "user@example.com", "What is in the doc?");

        assertNotNull(response);
        assertEquals("doc-123", response.getDocumentId());
        assertEquals("This is the grounded answer.", response.getAnswer());
        assertEquals(1, response.getRelevantSources().size());
    }

    @Test
    @DisplayName("Should reject chat query if document processing is not yet completed")
    void chatWithDocument_NotCompleted_ThrowsException() {
        Document processingDoc = Document.builder()
                .id("doc-pending")
                .uploadedBy("user@example.com")
                .processingStatus("PROCESSING")
                .build();

        when(documentRepository.findById("doc-pending")).thenReturn(Optional.of(processingDoc));

        assertThrows(InvalidFileException.class, () -> documentService.chatWithDocument("doc-pending", "user@example.com", "Test?"));
    }

    @Test
    @DisplayName("Should successfully delete document and cleanup disk file")
    void deleteDocument_Success() throws IOException {
        File fileOnDisk = new File(tempDir, "123_test.pdf");
        fileOnDisk.createNewFile();

        when(documentRepository.findById("doc-123")).thenReturn(Optional.of(sampleDoc));
        doNothing().when(documentRepository).delete(sampleDoc);

        documentService.deleteDocument("doc-123", "user@example.com");

        verify(documentRepository).delete(sampleDoc);
    }
}
