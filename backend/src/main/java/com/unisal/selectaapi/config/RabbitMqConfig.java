package com.unisal.selectaapi.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange("email.exchange");
    }

    @Bean
    public Queue queue() {
        return new Queue("email.confirmacao", true, false, false,
                Map.of("x-dead-letter-routing-key", "email.confirmacao.dlq.key",
                        "x-dead-letter-exchange", "email.exchange"));
    }

    @Bean
    public Queue queueDlq() {
        return new Queue("email.confirmacao.dlq", true, false, false, null);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(exchange()).with("email.confirmacao.key");
    }

    @Bean
    public Binding bindingDlq() {
        return BindingBuilder.bind(queueDlq()).to(exchange()).with("email.confirmacao.dlq.key");
    }

    @Bean
    public Queue candidaturaQueue() {
        return new Queue("candidatura.atualizacao", true, false, false,
                Map.of("x-dead-letter-routing-key", "candidatura.atualizacao.dlq.key",
                        "x-dead-letter-exchange", "email.exchange"));
    }

    @Bean
    public Queue candidaturaQueueDlq() {
        return new Queue("candidatura.atualizacao.dlq", true, false, false, null);
    }

    @Bean
    public Binding candidaturaBinding() {
        return BindingBuilder.bind(candidaturaQueue()).to(exchange()).with("candidatura.atualizacao.key");
    }

    @Bean
    public Binding candidaturaBindingDlq() {
        return BindingBuilder.bind(candidaturaQueueDlq()).to(exchange()).with("candidatura.atualizacao.dlq.key");
    }

    public static final String EXCHANGE = "email.exchange";
    public static final String EMBEDDING_QUEUE = "embedding.atualizacao";
    public static final String EMBEDDING_ROUTING_KEY = "embedding.atualizacao.key";
    private static final String EMBEDDING_DLQ = "embedding.atualizacao.dlq";
    private static final String EMBEDDING_DLQ_ROUTING_KEY = "embedding.atualizacao.dlq.key";

    @Bean
    public Queue embeddingQueue() {
        return new Queue(EMBEDDING_QUEUE, true, false, false,
                Map.of("x-dead-letter-routing-key", EMBEDDING_DLQ_ROUTING_KEY,
                        "x-dead-letter-exchange", EXCHANGE));
    }

    @Bean
    public Queue embeddingQueueDlq() {
        return new Queue(EMBEDDING_DLQ, true, false, false, null);
    }

    @Bean
    public Binding embeddingBinding() {
        return BindingBuilder.bind(embeddingQueue()).to(exchange()).with(EMBEDDING_ROUTING_KEY);
    }

    @Bean
    public Binding embeddingBindingDlq() {
        return BindingBuilder.bind(embeddingQueueDlq()).to(exchange()).with(EMBEDDING_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
