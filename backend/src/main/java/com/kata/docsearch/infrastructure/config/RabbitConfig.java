package com.kata.docsearch.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PROCESSING_QUEUE = "documents.processing";
    public static final String PROCESSING_DLX = "documents.processing.dlx";
    public static final String PROCESSING_DLQ = "documents.processing.dlq";
    public static final String EVENTS_EXCHANGE = "documents.events";

    /** Fanout: every bound queue receives every event. */
    @Bean
    FanoutExchange documentEventsExchange() {
        return new FanoutExchange(EVENTS_EXCHANGE, true, false);
    }

    /**
     * One server-named, exclusive, auto-delete queue per API instance. Each instance receives every
     * event and relays it to its own SSE clients; the queue disappears when the instance stops.
     */
    @Bean
    Queue documentEventsQueue() {
        return new AnonymousQueue();
    }

    @Bean
    Binding documentEventsBinding(Queue documentEventsQueue, FanoutExchange documentEventsExchange) {
        return BindingBuilder.bind(documentEventsQueue).to(documentEventsExchange);
    }

    /** Durable queue: jobs survive restarts. If retries are exhausted, the message goes to the DLQ. */
    @Bean
    Queue processingQueue() {
        return QueueBuilder.durable(PROCESSING_QUEUE)
                .deadLetterExchange(PROCESSING_DLX)
                .deadLetterRoutingKey(PROCESSING_DLQ)
                .build();
    }

    @Bean
    DirectExchange processingDeadLetterExchange() {
        return new DirectExchange(PROCESSING_DLX, true, false);
    }

    @Bean
    Queue processingDeadLetterQueue() {
        return QueueBuilder.durable(PROCESSING_DLQ).build();
    }

    @Bean
    Binding processingDeadLetterBinding(Queue processingDeadLetterQueue, DirectExchange processingDeadLetterExchange) {
        return BindingBuilder.bind(processingDeadLetterQueue).to(processingDeadLetterExchange).with(PROCESSING_DLQ);
    }
}