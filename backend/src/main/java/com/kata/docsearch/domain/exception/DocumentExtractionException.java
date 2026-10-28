package com.kata.docsearch.domain.exception;
/** The read of the file is failing (corrupt, protected, etc.). Retry won't help. */
public class DocumentExtractionException extends DomainException{
    public DocumentExtractionException(String message) {
        super(message);
    }
}
