package com.blamex321.document_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AIResponse {
	private String summary;
	private String classification;
	private String riskScore;
}
