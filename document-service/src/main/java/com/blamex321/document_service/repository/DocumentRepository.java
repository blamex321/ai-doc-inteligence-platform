package com.blamex321.document_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.blamex321.document_service.model.Document;

public interface DocumentRepository extends MongoRepository<Document, String> {
    List<Document> findByUploadedBy(String email);
    Page<Document> findByUploadedBy(String email, Pageable pageable);
}
