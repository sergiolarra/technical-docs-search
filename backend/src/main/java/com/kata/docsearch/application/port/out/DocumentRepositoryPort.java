package com.kata.docsearch.application.port.out;

import com.kata.docsearch.domain.model.Document;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {
    Document save(Document document);

    Optional<Document> findById(UUID id);

    /** Loads the document locking its row until the end of the transaction (prevents processing it twice). */
    Optional<Document> findByIdForUpdate(UUID id);
}
