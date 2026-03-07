package com.blamex321.ai_service.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1/chat/completions")
            .build();

    public String analyzeText(String text) {
    	
    	if (text.length() > 4000) {
    	    text = text.substring(0, 4000);
    	}

        String prompt = """
        Analyze this document and return JSON with:
        summary,
        classification (Resume, Financial, Legal, Sensitive, General),
        riskScore (Low, Medium, High)

        Document:
        """ + text;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", new Object[]{
                        Map.of("role", "user", "content", prompt)
                }
        );

        Mono<String> response = webClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);

        return response.block();
    }
}