package com.kata.docsearch.domain.exception;

import com.kata.docsearch.domain.model.DocumentStatus;

import java.util.UUID;

public class InvalidDocumentStateException extends DomainException{
    public InvalidDocumentStateException(UUID id, DocumentStatus current) {
        super("Document with ID: " + id + " can't change state from: " + current);
    }
}
