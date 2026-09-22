package com.blamex321.document_service.service;

import java.io.File;
import java.io.IOException;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);
    private final Tika tika = new Tika();

    public String extractText(File file) throws IOException, TikaException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist for text extraction");
        }

        log.info("Extracting text from file: {} (size: {} bytes)", file.getName(), file.length());
        String extracted = tika.parseToString(file);
        log.info("Successfully extracted {} characters from {}", extracted != null ? extracted.length() : 0, file.getName());
        return extracted != null ? extracted : "";
    }
}
