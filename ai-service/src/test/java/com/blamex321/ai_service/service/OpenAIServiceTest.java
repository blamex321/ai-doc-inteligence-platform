package com.blamex321.ai_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.blamex321.ai_service.dto.AIChatResponse;
import com.blamex321.ai_service.dto.AIResponse;

class OpenAIServiceTest {

    private OpenAIService openAIService;

    @BeforeEach
    void setUp() {
        openAIService = new OpenAIService();
        // Leave apiKey blank to test fallback mock logic
        ReflectionTestUtils.setField(openAIService, "apiKey", "");
        ReflectionTestUtils.setField(openAIService, "model", "gpt-4o-mini");
    }

    @Test
    @DisplayName("Should handle empty or blank text safely")
    void analyzeText_EmptyText() {
        AIResponse response = openAIService.analyzeText("");

        assertNotNull(response);
        assertEquals("Document contains no readable text.", response.getSummary());
        assertEquals("General", response.getClassification());
        assertEquals("Low", response.getRiskScore());
    }

    @Test
    @DisplayName("Should accurately classify resume documents in fallback mode")
    void analyzeText_ResumeFallback() {
        String text = "John Doe. Experience: 5 years Java Software Engineer. Education: B.Tech Computer Science. Skills: Spring Boot, MongoDB.";
        AIResponse response = openAIService.analyzeText(text);

        assertNotNull(response);
        assertEquals("Resume", response.getClassification());
        assertEquals("Low", response.getRiskScore());
        assertTrue(response.getKeywords().contains("resume"));
    }

    @Test
    @DisplayName("Should accurately classify financial documents in fallback mode")
    void analyzeText_FinancialFallback() {
        String text = "Annual Invoice: Total balance sheet revenue shows payment tax due of $50,000.";
        AIResponse response = openAIService.analyzeText(text);

        assertNotNull(response);
        assertEquals("Financial", response.getClassification());
        assertEquals("Medium", response.getRiskScore());
    }

    @Test
    @DisplayName("Should accurately classify legal documents in fallback mode")
    void analyzeText_LegalFallback() {
        String text = "Non-Disclosure Agreement: Confidential proprietary terms and liability clauses.";
        AIResponse response = openAIService.analyzeText(text);

        assertNotNull(response);
        assertEquals("Legal", response.getClassification());
        assertEquals("High", response.getRiskScore());
    }

    @Test
    @DisplayName("Should handle long text safely without exceeding boundaries")
    void analyzeText_LongText_SafelyTruncated() {
        String longText = "Experience and resume details. ".repeat(2000);
        AIResponse response = openAIService.analyzeText(longText);

        assertNotNull(response);
        assertEquals("Resume", response.getClassification());
    }

    @Test
    @DisplayName("Should handle RAG document Q&A and return relevant sources")
    void askDocumentQuestion_Success() {
        String document = "Employee Agreement. Clause 1: Working hours are 9 AM to 5 PM. "
                + "Clause 2: Notice period is 60 calendar days upon resignation. "
                + "Clause 3: Confidentiality remains binding for 2 years.";

        AIChatResponse response = openAIService.askDocumentQuestion(document, "What is the notice period?");

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertFalse(response.getRelevantSources().isEmpty());
        assertTrue(response.getAnswer().toLowerCase().contains("notice") || response.getAnswer().toLowerCase().contains("60"));
    }

    @Test
    @DisplayName("Should return safe fallback message if question or document is blank")
    void askDocumentQuestion_BlankInputs() {
        AIChatResponse response = openAIService.askDocumentQuestion("", "Any question?");
        assertNotNull(response);
        assertEquals("The document contains no readable text to answer questions.", response.getAnswer());

        AIChatResponse emptyQResponse = openAIService.askDocumentQuestion("Sample doc content", "");
        assertNotNull(emptyQResponse);
        assertEquals("Please provide a question about the document.", emptyQResponse.getAnswer());
    }
}
