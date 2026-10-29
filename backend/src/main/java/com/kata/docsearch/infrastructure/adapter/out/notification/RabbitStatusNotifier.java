package com.kata.docsearch.infrastructure.adapter.out.notification;

import com.kata.docsearch.application.port.out.StatusNotifierPort;
import com.kata.docsearch.domain.event.DocumentStatusChanged;
import com.kata.docsearch.domain.model.Document;
import com.kata.docsearch.infrastructure.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
class RabbitStatusNotifier implements StatusNotifierPort {

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    /**
     * Publishes only after the transaction commits: a browser that reacts to the event and reads
     * the document must already see the new state. A rolled-back transaction publishes nothing.
     */
    @Override
    public void documentStatusChanged(Document document) {
        var event = DocumentStatusChanged.from(document);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(event);
                }
            });
        } else {
            publish(event);
        }
    }

    /** A lost notification must not undo the processing: clients re-read the state when they reconnect. */
    private void publish(DocumentStatusChanged event) {
        try {
            var properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            rabbitTemplate.send(RabbitConfig.EVENTS_EXCHANGE, "",
                    new Message(jsonMapper.writeValueAsBytes(event), properties));
        } catch (AmqpException e) {
            log.warn("Could not publish status event for document {}", event.documentId(), e);
        }
    }
}