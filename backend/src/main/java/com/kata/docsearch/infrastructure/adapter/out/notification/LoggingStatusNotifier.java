package com.kata.docsearch.infrastructure.adapter.out.notification;

import com.kata.docsearch.application.port.out.StatusNotifierPort;
import com.kata.docsearch.domain.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class LoggingStatusNotifier implements StatusNotifierPort {

    @Override
    public void documentStatusChanged(Document document) {
        log.info("Document {} changed to {}", document.getId(), document.getStatus());
    }
}