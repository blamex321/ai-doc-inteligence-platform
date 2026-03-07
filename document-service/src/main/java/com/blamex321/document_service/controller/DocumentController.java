package com.blamex321.document_service.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.blamex321.document_service.model.Document;
import com.blamex321.document_service.repository.DocumentRepository;
import com.blamex321.document_service.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
	private final DocumentService documentService;
	@Autowired
	private DocumentRepository documentRepository;

	@PostMapping("/upload")
	public Document uploadDocument(@RequestHeader("X-User-Email") String email,
			@RequestParam("file") MultipartFile file) throws IOException, TikaException {

		return documentService.upload(file, email);
	}

	@GetMapping("/my")
	public List<Document> getMyDocuments(@RequestHeader("X-User-Email") String email) {
		return documentService.findByUserEmail(email);
	}

	@GetMapping("/{id}")
	public Document getById(@PathVariable String id, @RequestHeader("X-User-Email") String email) {

		Document doc = documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));

		if (!doc.getUploadedBy().equals(email)) {
			throw new RuntimeException("Access denied");
		}

		return doc;
	}

	@GetMapping("/{id}/download")
	public byte[] downloadDocument(@PathVariable String id, @RequestHeader("X-User-Email") String email)
			throws IOException {
		Document doc = documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));

		if (!doc.getUploadedBy().equals(email)) {
			throw new RuntimeException("Access denied");
		}

		String filePath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator
				+ doc.getFilePath();
		return java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));

	}	
	@GetMapping("/{id}/analysis")
	public Map<String, Object> getAnalysis(
	        @PathVariable String id,
	        @RequestHeader("X-User-Email") String email
	) {

	    Document doc = documentRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Document not found"));

	    if (!doc.getUploadedBy().equals(email)) {
	        throw new RuntimeException("Access denied");
	    }

	    return Map.of(
	            "status", doc.getProcessingStatus(),
	            "summary", doc.getSummary(),
	            "classification", doc.getClassification(),
	            "riskScore", doc.getRiskScore()
	    );
	}
}
