package com.kata.docsearch.infrastructure.adapter.in.messaging;

import com.kata.docsearch.application.port.in.ProcessDocumentUseCase;
import com.kata.docsearch.infrastructure.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class DocumentProcessingListener {

    private final ProcessDocumentUseCase processDocument;

    /** If it throws an exception, Spring retries (see application.yml) and then sends it to the DLQ. */
    @RabbitListener(queues = RabbitConfig.PROCESSING_QUEUE)
    void onDocumentUploaded(String documentId) {
        processDocument.process(UUID.fromString(documentId));
    }

    /** Once retries are exhausted: the document cannot remain in PROCESSING forever. */
    @RabbitListener(queues = RabbitConfig.PROCESSING_DLQ)
    void onProcessingExhausted(String documentId) {
        log.error("Document {} exhausted its processing retries", documentId);
        processDocument.markAsFailed(UUID.fromString(documentId), "Processing failed after several attempts");
    }
}