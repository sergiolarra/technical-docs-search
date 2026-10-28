package com.kata.docsearch.application.port.in;

import java.util.UUID;

public interface ProcessDocumentUseCase {
    void process(UUID documentId);

    void markAsFailed(UUID documentId, String reason);
}
