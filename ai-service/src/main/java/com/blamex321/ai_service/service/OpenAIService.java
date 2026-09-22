package com.blamex321.ai_service.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.blamex321.ai_service.dto.AIChatResponse;
import com.blamex321.ai_service.dto.AIResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class OpenAIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private static final int MAX_DOCUMENT_CHARS = 16000;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from",
            "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him",
            "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "me", "more",
            "most", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other",
            "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "should", "so",
            "some", "such", "than", "that", "the", "their", "theirs", "them", "themselves", "then",
            "there", "these", "they", "this", "those", "through", "to", "too", "under", "until", "up",
            "very", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why",
            "with", "would", "you", "your", "yours", "yourself", "yourselves"
    );

    public OpenAIService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public AIResponse analyzeText(String text) {
        if (text == null || text.isBlank()) {
            return AIResponse.builder()
                    .summary("Document contains no readable text.")
                    .classification("General")
                    .riskScore("Low")
                    .keywords(Collections.emptyList())
                    .build();
        }

        String sanitized = sanitizeText(text);

        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            log.warn("OPENAI_API_KEY is not configured. Returning fallback mock response.");
            return generateMockAnalysis(sanitized);
        }

        String systemPrompt = """
            You are an expert Document Intelligence Assistant. Analyze the provided document text carefully.
            You MUST return a valid JSON object matching this schema exactly:
            {
              "summary": "Concise executive summary of 2 to 4 sentences highlighting the main purpose, parties, and key numbers/dates.",
              "classification": "One of: Resume, Financial, Legal, Technical, Medical, General",
              "riskScore": "One of: Low, Medium, High",
              "keywords": ["array", "of", "top", "5", "identified", "concepts"]
            }
            Do not include markdown code block formatting (like ```json). Just the raw JSON object.
            """;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", sanitized)
                ),
                "response_format", Map.of("type", "json_object"),
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
            JsonNode rootNode = objectMapper.readTree(rawJson);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();

            JsonNode parsed = objectMapper.readTree(content);
            String summary = parsed.path("summary").asText("No summary provided.");
            String classification = parsed.path("classification").asText("General");
            String riskScore = parsed.path("riskScore").asText("Low");

            List<String> keywords = new ArrayList<>();
            JsonNode kwNode = parsed.path("keywords");
            if (kwNode.isArray()) {
                for (JsonNode k : kwNode) {
                    keywords.add(k.asText());
                }
            }

            return AIResponse.builder()
                    .summary(summary)
                    .classification(classification)
                    .riskScore(riskScore)
                    .keywords(keywords)
                    .build();

        } catch (Exception e) {
            log.error("Error communicating with OpenAI: {}", e.getMessage(), e);
            return generateMockAnalysis(sanitized);
        }
    }

    public AIChatResponse askDocumentQuestion(String documentText, String question) {
        if (documentText == null || documentText.isBlank()) {
            return AIChatResponse.builder()
                    .answer("The document contains no readable text to answer questions.")
                    .relevantSources(Collections.emptyList())
                    .model("none")
                    .build();
        }

        if (question == null || question.isBlank()) {
            return AIChatResponse.builder()
                    .answer("Please provide a question about the document.")
                    .relevantSources(Collections.emptyList())
                    .model("none")
                    .build();
        }

        // 1. Chunk document into semantic overlapping sections
        List<String> chunks = splitIntoChunks(documentText, 800, 200);

        // 2. Rank chunks by relevance to the query with intelligent intent boost
        List<String> topChunks = rankChunksByRelevance(chunks, question, 3);

        // 3. Fallback if no OpenAI API Key configured
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            return generateMockRagAnswer(topChunks, question, documentText);
        }

        // 4. Grounded Prompt with source excerpts
        String systemPrompt = """
            You are an expert Document Intelligence Assistant.
            Answer the user's question accurately, concisely, and factually based STRICTLY on the provided Document Excerpts.
            Guidelines:
            - Ground your answer ONLY in the facts mentioned in the excerpts.
            - Do NOT hallucinate or assume facts not present in the excerpts.
            - If the excerpts do not contain enough information to answer, state clearly: "The provided document does not contain sufficient information to answer this question."
            - Provide a direct, professional, and well-structured answer (using bullet points where appropriate).
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
                    .model("gpt-4o-mini")
                    .build();

        } catch (Exception e) {
            log.error("Error communicating with OpenAI for RAG Q&A: {}", e.getMessage(), e);
            return generateMockRagAnswer(topChunks, question, documentText);
        }
    }

    List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        // Normalize multiple blank lines to double newline, but preserve single line/paragraph breaks
        String normalized = text.replaceAll("[ \\t]+", " ")
                .replaceAll("\\r\\n|\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (normalized.length() <= chunkSize) {
            chunks.add(normalized);
            return chunks;
        }

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            if (end < normalized.length()) {
                // Try to find a paragraph break first
                int lastPara = normalized.lastIndexOf("\n\n", end);
                if (lastPara > start + chunkSize / 3) {
                    end = lastPara;
                } else {
                    // Try to find a sentence end (. or \n)
                    int lastSentence = Math.max(normalized.lastIndexOf(". ", end), normalized.lastIndexOf("\n", end));
                    if (lastSentence > start + chunkSize / 3) {
                        end = lastSentence + 1;
                    } else {
                        // Otherwise break at word boundary
                        int lastSpace = normalized.lastIndexOf(' ', end);
                        if (lastSpace > start + chunkSize / 3) {
                            end = lastSpace;
                        }
                    }
                }
            }
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) break;
            start = Math.max(start + 1, end - overlap);
        }
        return chunks;
    }

    List<String> rankChunksByRelevance(List<String> chunks, String question, int topK) {
        if (chunks.size() <= topK) return chunks;

        String qLower = question.toLowerCase(Locale.ROOT);
        Set<String> queryWords = Arrays.stream(qLower.split("[^a-zA-Z0-9]+"))
                .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());

        boolean isExperienceQuery = qLower.contains("experience") || qLower.contains("work") || qLower.contains("role")
                || qLower.contains("job") || qLower.contains("career") || qLower.contains("background")
                || qLower.contains("candidate") || qLower.contains("employment") || qLower.contains("history");

        boolean isSkillsQuery = qLower.contains("skill") || qLower.contains("tech") || qLower.contains("stack")
                || qLower.contains("tool") || qLower.contains("framework") || qLower.contains("language");

        boolean isEducationQuery = qLower.contains("education") || qLower.contains("degree") || qLower.contains("university")
                || qLower.contains("college") || qLower.contains("bachelor") || qLower.contains("master") || qLower.contains("gpa");

        List<Map.Entry<String, Double>> scoredChunks = new ArrayList<>();
        for (String chunk : chunks) {
            String lower = chunk.toLowerCase(Locale.ROOT);
            double score = 0.0;

            // 1. Term frequency scoring
            for (String word : queryWords) {
                int count = countOccurrences(lower, word);
                score += count * 2.0;
            }

            // 2. Exact phrase bonus
            if (lower.contains(qLower)) {
                score += 15.0;
            }

            // 3. Section and Intent boosting
            if (isExperienceQuery) {
                if (lower.contains("work experience") || lower.contains("professional experience")
                        || lower.contains("employment history") || lower.contains("experience")) {
                    score += 8.0;
                }
                if (lower.contains("software engineer") || lower.contains("developer") || lower.contains("architect")
                        || lower.contains("lead") || lower.contains("responsibilities") || lower.contains("full-stack")) {
                    score += 4.0;
                }
                // Temporal tenure boost (e.g. 2022 - Present, 2021 - 2023)
                if (lower.matches(".*\\b(20[12][0-9]|present|years|months)\\b.*")) {
                    score += 4.0;
                }
                // Penalize contact-only chunks for experience queries
                if ((lower.contains("@gmail.com") || lower.contains("phone") || lower.contains("linkedin.com") || lower.contains("github.com"))
                        && !lower.contains("responsibilities") && !lower.contains("contributed")) {
                    score -= 5.0;
                }
            }

            if (isSkillsQuery) {
                if (lower.contains("technical skills") || lower.contains("skills") || lower.contains("technologies")
                        || lower.contains("languages") || lower.contains("frameworks")) {
                    score += 8.0;
                }
            }

            if (isEducationQuery) {
                if (lower.contains("education") || lower.contains("bachelor") || lower.contains("master")
                        || lower.contains("b.tech") || lower.contains("degree") || lower.contains("university")) {
                    score += 8.0;
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

    private int countOccurrences(String text, String word) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(word, idx)) != -1) {
            count++;
            idx += word.length();
        }
        return count;
    }

    private AIChatResponse generateMockRagAnswer(List<String> topChunks, String question, String fullText) {
        if (topChunks.isEmpty() || topChunks.get(0).isBlank()) {
            return AIChatResponse.builder()
                    .answer("The document does not contain sufficient details to answer: \"" + question + "\".")
                    .relevantSources(Collections.emptyList())
                    .model("local-heuristic-engine")
                    .build();
        }

        String qLower = question.toLowerCase(Locale.ROOT);
        boolean isExperienceQuery = qLower.contains("experience") || qLower.contains("work") || qLower.contains("role")
                || qLower.contains("job") || qLower.contains("career") || qLower.contains("background")
                || qLower.contains("candidate") || qLower.contains("employment");

        boolean isSkillsQuery = qLower.contains("skill") || qLower.contains("tech") || qLower.contains("stack")
                || qLower.contains("tool") || qLower.contains("framework") || qLower.contains("language");

        boolean isEducationQuery = qLower.contains("education") || qLower.contains("degree") || qLower.contains("university")
                || qLower.contains("college") || qLower.contains("academics") || qLower.contains("bachelor") || qLower.contains("master");

        // Attempt to extract candidate name or main title
        String candidateName = extractCandidateName(fullText);

        StringBuilder sb = new StringBuilder();

        if (isExperienceQuery) {
            sb.append("### Experience Profile");
            if (candidateName != null && !candidateName.isBlank()) {
                sb.append(" for **").append(candidateName).append("**");
            }
            sb.append("\n\n");

            // Extract experience bullet points from top chunks
            List<String> experienceHighlights = extractHighlights(topChunks, Set.of("experience", "developer", "engineer", "built", "developed", "managed", "designed", "architected", "worked", "led", "contributed", "implemented", "full-stack"));

            if (!experienceHighlights.isEmpty()) {
                sb.append("**Key Experience & Technical Roles:**\n");
                for (String hl : experienceHighlights) {
                    sb.append("• ").append(hl).append("\n");
                }
            } else {
                sb.append(cleanSnippet(topChunks.get(0), 300)).append("\n");
            }
            sb.append("\n> _*Note:_* _Generated via Local Semantic RAG Fallback. To enable conversational reasoning with OpenAI GPT-4o-mini, configure `OPENAI_API_KEY`._");

        } else if (isSkillsQuery) {
            sb.append("### Technical Skills & Competencies\n\n");
            List<String> skillHighlights = extractHighlights(topChunks, Set.of("skill", "java", "python", "spring", "react", "mongo", "docker", "aws", "git", "sql", "api", "cloud"));
            if (!skillHighlights.isEmpty()) {
                for (String hl : skillHighlights) {
                    sb.append("• ").append(hl).append("\n");
                }
            } else {
                sb.append(cleanSnippet(topChunks.get(0), 300)).append("\n");
            }
            sb.append("\n> _*Note:_* _Generated via Local Semantic RAG Fallback. Configure `OPENAI_API_KEY` for GPT-4o-mini._");

        } else if (isEducationQuery) {
            sb.append("### Educational Background\n\n");
            List<String> eduHighlights = extractHighlights(topChunks, Set.of("education", "university", "college", "bachelor", "master", "degree", "b.tech", "gpa", "graduated"));
            if (!eduHighlights.isEmpty()) {
                for (String hl : eduHighlights) {
                    sb.append("• ").append(hl).append("\n");
                }
            } else {
                sb.append(cleanSnippet(topChunks.get(0), 300)).append("\n");
            }
            sb.append("\n> _*Note:_* _Generated via Local Semantic RAG Fallback. Configure `OPENAI_API_KEY` for GPT-4o-mini._");

        } else {
            // General query: extract sentences containing query keywords
            Set<String> queryWords = Arrays.stream(qLower.split("[^a-zA-Z0-9]+"))
                    .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
                    .collect(Collectors.toSet());

            List<String> highlights = extractHighlights(topChunks, queryWords);
            if (!highlights.isEmpty()) {
                sb.append("Based on the document excerpts for **\"").append(question).append("\"**:\n\n");
                for (String hl : highlights) {
                    sb.append("• ").append(hl).append("\n");
                }
            } else {
                sb.append("Regarding **\"").append(question).append("\"**:\n\n");
                sb.append(cleanSnippet(topChunks.get(0), 300)).append("\n");
            }
            sb.append("\n> _*Note:_* _Generated via Local Semantic RAG Fallback. Configure `OPENAI_API_KEY` for GPT-4o-mini._");
        }

        return AIChatResponse.builder()
                .answer(sb.toString())
                .relevantSources(topChunks)
                .model("local-heuristic-engine")
                .build();
    }

    private static final Set<String> SECTION_HEADERS = Set.of(
            "work experience", "experience", "professional experience", "employment history",
            "education", "skills", "technical skills", "summary", "professional summary"
    );

    private String extractCandidateName(String fullText) {
        if (fullText == null || fullText.isBlank()) return null;
        String[] lines = fullText.split("\\n+");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;
            // Take the first segment before pipe or comma if it looks like a person's name
            String firstPart = trimmed.split("[|,\\n]")[0].trim();
            String lower = firstPart.toLowerCase(Locale.ROOT);
            if (lower.equals("professional summary") || lower.equals("summary")
                    || lower.equals("work experience") || lower.equals("experience")
                    || lower.equals("curriculum vitae") || lower.equals("resume")
                    || lower.equals("skills") || lower.equals("education")
                    || lower.equals("technical skills")
                    || firstPart.contains("@") || firstPart.length() < 3 || firstPart.length() > 40) {
                continue;
            }
            return firstPart;
        }
        return null;
    }

    private List<String> extractHighlights(List<String> chunks, Set<String> targetTerms) {
        List<String> highlights = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String chunk : chunks) {
            // Split chunk into lines or sentences
            String[] segments = chunk.split("(\\n+|(?<=\\.)\\s+)");
            for (String segment : segments) {
                String clean = segment.trim().replaceAll("^[•\\-*\\d.]+\\s*", "");
                if (clean.length() < 15 || clean.length() > 280) continue;
                String lower = clean.toLowerCase(Locale.ROOT);

                // Skip generic section headings
                if (SECTION_HEADERS.contains(lower)) continue;

                // Check if segment contains any of the target terms
                boolean matches = false;
                for (String term : targetTerms) {
                    if (lower.contains(term.toLowerCase(Locale.ROOT))) {
                        matches = true;
                        break;
                    }
                }

                if (matches && !seen.contains(clean)) {
                    seen.add(clean);
                    highlights.add(clean);
                    if (highlights.size() >= 4) break;
                }
            }
            if (highlights.size() >= 4) break;
        }

        return highlights;
    }

    private String cleanSnippet(String text, int maxLen) {
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= maxLen) return cleaned;
        int lastSpace = cleaned.lastIndexOf(' ', maxLen);
        if (lastSpace > maxLen / 2) {
            return cleaned.substring(0, lastSpace) + "...";
        }
        return cleaned.substring(0, maxLen) + "...";
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

    private AIResponse generateMockAnalysis(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        String classification = "General";
        String riskScore = "Low";
        List<String> keywords = new ArrayList<>();

        if (lower.contains("resume") || lower.contains("curriculum vitae") || lower.contains("experience") || lower.contains("education")) {
            classification = "Resume";
            riskScore = "Low";
            keywords.addAll(List.of("resume", "career", "skills", "experience", "education"));
        } else if (lower.contains("invoice") || lower.contains("payment") || lower.contains("balance sheet") || lower.contains("financial") || lower.contains("revenue")) {
            classification = "Financial";
            riskScore = "Medium";
            keywords.addAll(List.of("financial", "invoice", "statement", "payment", "revenue"));
        } else if (lower.contains("contract") || lower.contains("agreement") || lower.contains("liability") || lower.contains("non-disclosure") || lower.contains("confidential")) {
            classification = "Legal";
            riskScore = "High";
            keywords.addAll(List.of("legal", "agreement", "liability", "clauses", "compliance"));
        } else if (lower.contains("architecture") || lower.contains("api") || lower.contains("database") || lower.contains("software") || lower.contains("docker")) {
            classification = "Technical";
            riskScore = "Low";
            keywords.addAll(List.of("technical", "software", "architecture", "microservices", "infrastructure"));
        } else {
            keywords.addAll(List.of("document", "content", "summary", "analysis"));
        }

        String summary = "Automated fallback analysis: Document classified as " + classification +
                " with a " + riskScore + " risk score profile based on extracted structural patterns.";

        return AIResponse.builder()
                .summary(summary)
                .classification(classification)
                .riskScore(riskScore)
                .keywords(keywords)
                .build();
    }
}