package com.kata.docsearch.application.port.in;

import com.kata.docsearch.domain.model.Document;

public interface UploadDocumentUseCase {
    Document upload(UploadDocumentCommand command);
}
