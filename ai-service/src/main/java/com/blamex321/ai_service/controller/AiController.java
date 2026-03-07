package com.blamex321.ai_service.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blamex321.ai_service.dto.AIRequest;
import com.blamex321.ai_service.dto.AIResponse;
import com.blamex321.ai_service.service.OpenAIService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class AiController {

    private final OpenAIService openAIService;

    @PostMapping("/analyze")
    public AIResponse analyze(@RequestBody AIRequest request) {

        String rawResponse = openAIService.analyzeText(request.getText());

        // Temporary: return raw response inside summary
        return AIResponse.builder()
                .summary(rawResponse)
                .classification("LLM-Generated")
                .riskScore("Dynamic")
                .build();
    }
}