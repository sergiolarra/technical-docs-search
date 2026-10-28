package com.kata.docsearch.application.port.out;

import com.kata.docsearch.domain.model.Document;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {
    Document save(Document document);

    Optional<Document> findById(UUID id);
}
