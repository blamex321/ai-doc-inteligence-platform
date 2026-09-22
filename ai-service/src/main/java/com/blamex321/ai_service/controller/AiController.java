package com.blamex321.ai_service.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blamex321.ai_service.dto.AIChatRequest;
import com.blamex321.ai_service.dto.AIChatResponse;
import com.blamex321.ai_service.dto.AIRequest;
import com.blamex321.ai_service.dto.AIResponse;
import com.blamex321.ai_service.service.OpenAIService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final OpenAIService openAIService;

    @PostMapping("/analyze")
    public AIResponse analyze(@RequestBody AIRequest request) {
        return openAIService.analyzeText(request.getText());
    }

    @PostMapping("/chat")
    public AIChatResponse chatWithDocument(@RequestBody AIChatRequest request) {
        return openAIService.askDocumentQuestion(request.getDocumentText(), request.getQuestion());
    }
}