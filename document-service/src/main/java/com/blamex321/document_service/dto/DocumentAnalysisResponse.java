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
public class DocumentAnalysisResponse {
    private String id;
    private String fileName;
    private String status;
    private String summary;
    private String classification;
    private String riskScore;
    private List<String> keywords;
}
