package com.kata.docsearch.domain.exception;

    public class InvalidSearchQueryException extends DomainException {
        public InvalidSearchQueryException(String message) {
            super(message);
        }
    }
