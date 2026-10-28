package com.kata.docsearch.infrastructure.adapter.out.persistence;

import com.kata.docsearch.application.port.out.DocumentRepositoryPort;
import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentMetadata;
import com.kata.docsearch.domain.model.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class DocumentPersistenceAdapter implements DocumentRepositoryPort {

    private final SpringDataDocumentRepository repository;

    @Override
    @Transactional
    public Document save(Document document) {
        var entity = repository.findById(document.getId()).orElseGet(DocumentJpaEntity::newInstance);
        copyToEntity(document, entity);
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<Document> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(this::toDomain);
    }

    private void copyToEntity(Document document, DocumentJpaEntity entity) {
        var metadata = document.getMetadata();
        var file = document.getFile();
        entity.setId(document.getId());
        entity.setTitle(metadata.title());
        entity.setAuthor(metadata.author());
        entity.setCategory(metadata.category());
        entity.setTags(metadata.tags());
        entity.setVersion(metadata.version());
        entity.setOriginalFilename(file.originalFilename());
        entity.setFormat(file.format());
        entity.setContentType(file.contentType());
        entity.setFileSize(file.size());
        entity.setStorageKey(file.storageKey());
        entity.setContent(document.getContent());
        entity.setStatus(document.getStatus());
        entity.setErrorMessage(document.getErrorMessage());
        entity.setCreatedAt(document.getCreatedAt());
        entity.setUpdatedAt(document.getUpdatedAt());
    }

    private Document toDomain(DocumentJpaEntity entity) {
        var metadata = new DocumentMetadata(entity.getTitle(), entity.getAuthor(), entity.getCategory(),
                entity.getTags(), entity.getVersion());
        var file = new StoredFile(entity.getOriginalFilename(), entity.getFormat(), entity.getContentType(),
                entity.getFileSize(), entity.getStorageKey());
        return Document.restore(entity.getId(), metadata, file, entity.getStatus(), entity.getContent(),
                entity.getErrorMessage(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}