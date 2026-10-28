package com.kata.docsearch.application.port.in;

import com.kata.docsearch.domain.model.Document;

import java.util.UUID;

public interface GetDocumentUseCase {
    Document getById(UUID id);
}
