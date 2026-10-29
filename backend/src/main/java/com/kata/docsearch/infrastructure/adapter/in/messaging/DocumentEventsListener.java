package com.kata.docsearch.infrastructure.adapter.in.messaging;

import com.kata.docsearch.domain.event.DocumentStatusChanged;
import com.kata.docsearch.infrastructure.adapter.in.web.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
class DocumentEventsListener {

    private final JsonMapper jsonMapper;
    private final SseEmitterRegistry sseEmitters;

    /** Single consumer per instance, so events reach the browser in the order they were published. */
    @RabbitListener(queues = "#{documentEventsQueue.name}", concurrency = "1")
    void onDocumentStatusChanged(Message message) {
        sseEmitters.broadcast(jsonMapper.readValue(message.getBody(), DocumentStatusChanged.class));
    }
}