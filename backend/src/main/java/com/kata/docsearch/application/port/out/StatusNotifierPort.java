package com.kata.docsearch.application.port.out;

import com.kata.docsearch.domain.model.Document;

public interface StatusNotifierPort {
    void documentStatusChanged(Document document);
}
