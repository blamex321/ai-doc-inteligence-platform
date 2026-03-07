package com.blamex321.document_service.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
public class Document {

    @Id
    private String id;

    private String fileName;

    private String fileType;

    private String filePath;

    private String uploadedBy;

    private LocalDateTime uploadedAt;
    
    private String extractedText;
    private String summary;
    private List<String> keywords;
    private String classification;
    private String riskScore;
    private String processingStatus;
    
}