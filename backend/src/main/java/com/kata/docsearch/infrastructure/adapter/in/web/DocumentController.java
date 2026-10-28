package com.kata.docsearch.infrastructure.adapter.in.web;

import com.kata.docsearch.application.port.in.GetDocumentUseCase;
import com.kata.docsearch.application.port.in.UploadDocumentCommand;
import com.kata.docsearch.application.port.in.UploadDocumentUseCase;
import com.kata.docsearch.domain.exception.DomainException;
import com.kata.docsearch.domain.exception.InvalidDocumentException;
import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentMetadata;
import com.kata.docsearch.infrastructure.adapter.in.web.dto.BatchMetadataForm;
import com.kata.docsearch.infrastructure.adapter.in.web.dto.BatchUploadItemResponse;
import com.kata.docsearch.infrastructure.adapter.in.web.dto.DocumentDetailResponse;
import com.kata.docsearch.infrastructure.adapter.in.web.dto.UploadMetadataForm;
import com.kata.docsearch.infrastructure.adapter.in.web.dto.UploadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
class DocumentController {

    private static final int MAX_BATCH_FILES = 20;

    private final UploadDocumentUseCase uploadDocument;
    private final GetDocumentUseCase getDocument;

    /** HU-01: responds 202 immediately; processing continues in the background. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file,
                                          @Valid @ModelAttribute UploadMetadataForm metadata) {
        Document document = uploadDocument.upload(toCommand(file, metadata.toDomain()));
        return ResponseEntity.accepted()
                .location(URI.create("/api/documents/" + document.getId()))
                .body(UploadResponse.from(document));
    }

    @PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<List<BatchUploadItemResponse>> uploadBatch(@RequestParam("files") List<MultipartFile> files,
                                                              @Valid @ModelAttribute BatchMetadataForm metadata) {
        if (files.size() > MAX_BATCH_FILES) {
            throw new InvalidDocumentException("A batch can contain at most " + MAX_BATCH_FILES + " files");
        }
        var results = files.stream().map(file -> uploadOne(file, metadata)).toList();
        return ResponseEntity.accepted().body(results);
    }

    @GetMapping("/{id}")
    DocumentDetailResponse getById(@PathVariable UUID id) {
        return DocumentDetailResponse.from(getDocument.getById(id));
    }

    private BatchUploadItemResponse uploadOne(MultipartFile file, BatchMetadataForm metadata) {
        String filename = cleanFilename(file);
        try {
            String title = StringUtils.stripFilenameExtension(filename);
            return BatchUploadItemResponse.accepted(uploadDocument.upload(toCommand(file, metadata.toDomain(title))));
        } catch (DomainException e) {
            return BatchUploadItemResponse.rejected(filename, e.getMessage());
        }
    }

    private UploadDocumentCommand toCommand(MultipartFile file, DocumentMetadata metadata) {
        return new UploadDocumentCommand(metadata, cleanFilename(file), file.getSize(), file::getInputStream);
    }

    /** Discards any path sent by the client and leaves only the file name. */
    private static String cleanFilename(MultipartFile file) {
        return StringUtils.getFilename(file.getOriginalFilename());
    }
}