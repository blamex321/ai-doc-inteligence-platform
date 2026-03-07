package com.blamex321.ai_service.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AIResponse {
	private String summary;
	private String classification;
	private String riskScore;
}
