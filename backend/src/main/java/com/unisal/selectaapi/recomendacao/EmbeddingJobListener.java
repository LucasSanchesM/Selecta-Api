package com.unisal.selectaapi.recomendacao;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.unisal.selectaapi.config.RabbitMqConfig;

@Component
public class EmbeddingJobListener {

    private final EmbeddingIndexer indexer;

    public EmbeddingJobListener(EmbeddingIndexer indexer) {
        this.indexer = indexer;
    }

    @RabbitListener(queues = RabbitMqConfig.EMBEDDING_QUEUE, concurrency = "1-2")
    public void onMessage(EmbeddingJobMessage message) {
        indexer.process(message);
    }
}
