package com.kata.docsearch.domain.exception;

import java.util.UUID;

public class DocumentNotFoundException extends DomainException{
    public DocumentNotFoundException(UUID id) {
        super("Document not found with ID: " + id);
    }
}
