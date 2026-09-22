package com.blamex321.ai_service.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.blamex321.ai_service.dto.AIChatResponse;
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

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "and", "is", "in", "it", "of", "to", "for", "with", "on", "that", "this",
            "are", "was", "as", "at", "by", "an", "be", "from", "or", "what", "which", "who",
            "how", "where", "when", "why", "does", "did", "can", "could", "should", "would"
    ));

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

    public AIChatResponse askDocumentQuestion(String documentText, String question) {
        if (documentText == null || documentText.isBlank()) {
            return AIChatResponse.builder()
                    .answer("The document contains no readable text to answer questions.")
                    .relevantSources(Collections.emptyList())
                    .build();
        }
        if (question == null || question.isBlank()) {
            return AIChatResponse.builder()
                    .answer("Please provide a question about the document.")
                    .relevantSources(Collections.emptyList())
                    .build();
        }

        // 1. Chunk document into semantic overlapping sections
        List<String> chunks = splitIntoChunks(documentText, 700, 150);

        // 2. Rank chunks by relevance to the query
        List<String> topChunks = rankChunksByRelevance(chunks, question, 3);

        // 3. Fallback if no OpenAI API Key configured
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            return generateMockRagAnswer(topChunks, question);
        }

        // 4. Grounded Prompt with source excerpts
        String systemPrompt = """
            You are an expert Document Intelligence Assistant.
            Answer the user's question accurately, concisely, and factually based STRICTLY on the provided Document Excerpts.
            Guidelines:
            - Ground your answer ONLY in the facts mentioned in the excerpts.
            - Do NOT hallucinate or assume facts not present in the excerpts.
            - If the excerpts do not contain enough information to answer, state clearly: "The provided document does not contain sufficient information to answer this question."
            - Provide a direct, professional answer.
            """;

        StringBuilder excerptsContext = new StringBuilder();
        for (int i = 0; i < topChunks.size(); i++) {
            excerptsContext.append("[Excerpt ").append(i + 1).append("]:\n")
                    .append(topChunks.get(i)).append("\n\n");
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Document Excerpts:\n" + excerptsContext + "\nUser Question: " + question)
                ),
                "temperature", 0.1
        );

        try {
            Mono<String> responseMono = webClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class);

            String rawJson = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(rawJson);
            String answer = rootNode.path("choices").get(0).path("message").path("content").asText();

            return AIChatResponse.builder()
                    .answer(answer)
                    .relevantSources(topChunks)
                    .build();

        } catch (Exception e) {
            log.error("Error communicating with OpenAI for RAG Q&A: {}", e.getMessage(), e);
            return generateMockRagAnswer(topChunks, question);
        }
    }

    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= chunkSize) {
            chunks.add(cleaned);
            return chunks;
        }

        int start = 0;
        while (start < cleaned.length()) {
            int end = Math.min(start + chunkSize, cleaned.length());
            if (end < cleaned.length()) {
                int lastSpace = cleaned.lastIndexOf(' ', end);
                if (lastSpace > start + chunkSize / 2) {
                    end = lastSpace;
                }
            }
            chunks.add(cleaned.substring(start, end).trim());
            if (end >= cleaned.length()) break;
            start = Math.max(start + 1, end - overlap);
        }
        return chunks;
    }

    private List<String> rankChunksByRelevance(List<String> chunks, String question, int topK) {
        if (chunks.size() <= topK) return chunks;

        Set<String> queryWords = Arrays.stream(question.toLowerCase().split("[^a-zA-Z0-9]+"))
                .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());

        List<Map.Entry<String, Double>> scoredChunks = new ArrayList<>();
        for (String chunk : chunks) {
            String lower = chunk.toLowerCase();
            double score = 0;
            for (String word : queryWords) {
                if (lower.contains(word)) {
                    score += 1.0;
                }
            }
            scoredChunks.add(Map.entry(chunk, score));
        }

        scoredChunks.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scoredChunks.size()); i++) {
            result.add(scoredChunks.get(i).getKey());
        }
        return result;
    }

    private AIChatResponse generateMockRagAnswer(List<String> topChunks, String question) {
        String answer;
        if (topChunks.isEmpty() || topChunks.get(0).isBlank()) {
            answer = "The document does not contain sufficient details to answer: \"" + question + "\".";
        } else {
            String snippet = topChunks.get(0);
            if (snippet.length() > 220) {
                snippet = snippet.substring(0, 220) + "...";
            }
            answer = "Based on document context regarding \"" + question + "\": \"" + snippet + "\".";
        }
        return AIChatResponse.builder()
                .answer(answer)
                .relevantSources(topChunks)
                .build();
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

                List<String> keywords = new ArrayList<>();
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