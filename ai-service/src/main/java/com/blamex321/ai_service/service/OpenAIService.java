package com.blamex321.ai_service.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.blamex321.ai_service.dto.AIResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);
    private static final int MAX_DOCUMENT_CHARS = 16000;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1/chat/completions")
            .build();

    public AIResponse analyzeText(String text) {
        if (text == null || text.isBlank()) {
            return AIResponse.builder()
                    .summary("Document contains no readable text.")
                    .classification("General")
                    .riskScore("Low")
                    .keywords(Collections.emptyList())
                    .build();
        }

        // Clean excess whitespace and truncate within safe token boundaries
        String sanitizedText = sanitizeText(text);

        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            log.warn("OPENAI_API_KEY is not configured. Returning fallback mock response.");
            return createMockResponse(sanitizedText);
        }

        String systemPrompt = """
            You are an expert Document Intelligence AI system.
            Analyze the provided document text and produce a precise analysis.
            You MUST respond with valid JSON matching exactly this schema:
            {
              "summary": "Clear, concise 2-4 sentence executive summary of the document",
              "classification": "One of: Resume, Financial, Legal, Technical, Sensitive, General",
              "riskScore": "One of: Low, Medium, High",
              "keywords": ["key concept 1", "key concept 2", "key concept 3", "key concept 4", "key concept 5"]
            }
            """;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Document content:\n" + sanitizedText)
                ),
                "temperature", 0.2
        );

        try {
            Mono<String> responseMono = webClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class);

            String rawJson = responseMono.block();
            return parseOpenAIResponse(rawJson);

        } catch (Exception e) {
            log.error("Error communicating with OpenAI API: {}", e.getMessage(), e);
            return AIResponse.builder()
                    .summary("AI Analysis temporarily unavailable: " + e.getMessage())
                    .classification("General")
                    .riskScore("Medium")
                    .keywords(List.of("error", "retry"))
                    .build();
        }
    }

    private String sanitizeText(String text) {
        String cleaned = text.trim().replaceAll("\\s+", " ");
        if (cleaned.length() > MAX_DOCUMENT_CHARS) {
            // Keep head and tail for context if document is long
            int headChars = MAX_DOCUMENT_CHARS * 3 / 4;
            int tailChars = MAX_DOCUMENT_CHARS / 4;
            return cleaned.substring(0, headChars) + "\n...[truncated]...\n" + cleaned.substring(cleaned.length() - tailChars);
        }
        return cleaned;
    }

    private AIResponse parseOpenAIResponse(String rawJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawJson);
            JsonNode choices = rootNode.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                JsonNode contentNode = objectMapper.readTree(content);

                String summary = contentNode.path("summary").asText("No summary generated.");
                String classification = contentNode.path("classification").asText("General");
                String riskScore = contentNode.path("riskScore").asText("Low");

                List<String> keywords = new java.util.ArrayList<>();
                JsonNode keywordsNode = contentNode.path("keywords");
                if (keywordsNode.isArray()) {
                    for (JsonNode kw : keywordsNode) {
                        keywords.add(kw.asText());
                    }
                }

                return AIResponse.builder()
                        .summary(summary)
                        .classification(classification)
                        .riskScore(riskScore)
                        .keywords(keywords)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to parse structured JSON from OpenAI: {}", e.getMessage());
        }

        return AIResponse.builder()
                .summary("Failed to parse structured output from OpenAI.")
                .classification("General")
                .riskScore("Low")
                .keywords(Collections.emptyList())
                .build();
    }

    private AIResponse createMockResponse(String text) {
        String lower = text.toLowerCase();
        String classification = "General";
        String riskScore = "Low";

        if (lower.contains("experience") || lower.contains("education") || lower.contains("skills") || lower.contains("resume") || lower.contains("curriculum vitae")) {
            classification = "Resume";
            riskScore = "Low";
        } else if (lower.contains("invoice") || lower.contains("balance sheet") || lower.contains("revenue") || lower.contains("tax") || lower.contains("payment")) {
            classification = "Financial";
            riskScore = "Medium";
        } else if (lower.contains("agreement") || lower.contains("confidential") || lower.contains("contract") || lower.contains("liability")) {
            classification = "Legal";
            riskScore = "High";
        }

        return AIResponse.builder()
                .summary("Analyzed document (" + text.length() + " chars): identified as " + classification + " document with preliminary " + riskScore + " risk rating.")
                .classification(classification)
                .riskScore(riskScore)
                .keywords(List.of(classification.toLowerCase(), "automated-analysis", "document-intelligence"))
                .build();
    }
}