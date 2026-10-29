package com.kata.docsearch.infrastructure.adapter.out.notification;

import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.domain.model.DocumentMetadata;
import com.kata.docsearch.domain.model.StoredFile;
import com.kata.docsearch.infrastructure.config.RabbitConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RabbitStatusNotifierTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitStatusNotifier notifier = new RabbitStatusNotifier(rabbitTemplate, JsonMapper.builder().build());

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishesOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        notifier.documentStatusChanged(indexedDocument());
        verifyNoInteractions(rabbitTemplate);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(rabbitTemplate).send(eq(RabbitConfig.EVENTS_EXCHANGE), eq(""), any(Message.class));
    }

    @Test
    void publishesNothingWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        notifier.documentStatusChanged(indexedDocument());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void sendsJsonPayloadWithTheNewStatus() {
        Document document = indexedDocument();

        notifier.documentStatusChanged(document);

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitConfig.EVENTS_EXCHANGE), eq(""), sent.capture());
        String body = new String(sent.getValue().getBody(), StandardCharsets.UTF_8);
        assertThat(sent.getValue().getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(body).contains(document.getId().toString()).contains("\"status\":\"INDEXADO\"");
    }

    @Test
    void brokerFailureDoesNotBreakProcessing() {
        doThrow(new AmqpException("broker down")).when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class));

        assertThatCode(() -> notifier.documentStatusChanged(indexedDocument())).doesNotThrowAnyException();
    }

    private static Document indexedDocument() {
        var now = Instant.parse("2026-10-28T10:00:00Z");
        var document = Document.newPending(UUID.randomUUID(),
                new DocumentMetadata("Guía", "Ana", "Backend", List.of(), "1.0"),
                new StoredFile("guia.md", DocumentFormat.MARKDOWN, "text/x-web-markdown", 6, "key.md"), now);
        document.markIndexed("contenido", now);
        return document;
    }
}