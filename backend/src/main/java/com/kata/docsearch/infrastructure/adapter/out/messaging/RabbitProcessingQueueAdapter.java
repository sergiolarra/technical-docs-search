package com.kata.docsearch.infrastructure.adapter.out.messaging;

import com.kata.docsearch.application.port.out.ProcessingQueuePort;
import com.kata.docsearch.infrastructure.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class RabbitProcessingQueueAdapter implements ProcessingQueuePort {

    private final RabbitTemplate rabbitTemplate;

    /** Only the ID travels: the message is small and the consumer always reads the current state from the database. */
    @Override
    public void enqueue(UUID documentId) {
        rabbitTemplate.convertAndSend(RabbitConfig.PROCESSING_QUEUE, documentId.toString());
    }
}