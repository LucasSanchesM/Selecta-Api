package com.unisal.selectaapi.recomendacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.unisal.selectaapi.config.RabbitMqConfig;
import com.unisal.selectaapi.recomendacao.config.EmbeddingProperties;

@Component
public class EmbeddingEventRelay {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingEventRelay.class);

    private final RabbitTemplate rabbitTemplate;
    private final EmbeddingProperties properties;

    public EmbeddingEventRelay(RabbitTemplate rabbitTemplate, EmbeddingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmbeddingRequested(EmbeddingRequestedEvent event) {
        if (!properties.isEnabled() || event.id() == null) {
            return;
        }
        enqueue(new EmbeddingJobMessage(event.target(), event.id()));
    }

    public void enqueue(EmbeddingJobMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE,
                    RabbitMqConfig.EMBEDDING_ROUTING_KEY,
                    message);
        } catch (AmqpException exception) {
            // The reconciler re-enqueues anything that was missed, so the request flow is never blocked.
            log.warn("Falha ao enfileirar embedding {} {}: {}", message.target(), message.id(), exception.getMessage());
        }
    }
}
