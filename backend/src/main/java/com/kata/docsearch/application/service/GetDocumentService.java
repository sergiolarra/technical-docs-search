package com.kata.docsearch.application.service;

import com.kata.docsearch.application.port.in.GetDocumentUseCase;
import com.kata.docsearch.application.port.out.DocumentRepositoryPort;
import com.kata.docsearch.domain.exception.DocumentNotFoundException;
import com.kata.docsearch.domain.model.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetDocumentService implements GetDocumentUseCase {

    private final DocumentRepositoryPort repository;

    @Override
    public Document getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }
}