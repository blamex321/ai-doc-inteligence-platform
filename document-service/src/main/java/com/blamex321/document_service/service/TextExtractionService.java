package com.blamex321.document_service.service;

import java.io.File;
import java.io.IOException;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

@Service
public class TextExtractionService {
	
	public String extractText(File file) throws IOException, TikaException {
		Tika tika = new Tika();
		return tika.parseToString(file);
	}
}
