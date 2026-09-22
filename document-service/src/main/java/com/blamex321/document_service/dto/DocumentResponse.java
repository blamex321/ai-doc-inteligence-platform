package com.blamex321.document_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private String id;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String processingStatus;
    private String summary;
    private String classification;
    private String riskScore;
    private List<String> keywords;
}
