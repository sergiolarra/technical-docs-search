package com.kata.docsearch.infrastructure.adapter.in.web.dto;

import com.kata.docsearch.domain.model.DocumentMetadata;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Metadata of an individual upload, sent as multipart form fields. */
public record UploadMetadataForm(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 150) String author,
        @NotBlank @Size(max = 100) String category,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> tags,
        @NotBlank @Size(max = 50) String version) {

    public DocumentMetadata toDomain() {
        return new DocumentMetadata(title, author, category, tags, version);
    }
}