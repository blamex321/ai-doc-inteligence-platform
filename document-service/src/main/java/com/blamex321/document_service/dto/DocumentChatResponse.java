package com.blamex321.document_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChatResponse {
    private String documentId;
    private String fileName;
    private String question;
    private String answer;
    private List<String> relevantSources;
}
