package com.unisal.selectaapi.mensageria;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.unisal.selectaapi.config.RabbitMqConfig;
import com.unisal.selectaapi.mensageria.dto.SecurityEmailRequest;
import com.unisal.selectaapi.mensageria.service.SecurityEmailService;

/** Ponto único para os e-mails de segurança da conta. */
@Component
public class SecurityMailer {

    private final RabbitTemplate rabbitTemplate;
    private final SecurityEmailService securityEmailService;

    public SecurityMailer(RabbitTemplate rabbitTemplate, SecurityEmailService securityEmailService) {
        this.rabbitTemplate = rabbitTemplate;
        this.securityEmailService = securityEmailService;
    }

    public void sendCode(String email, String subject, String title, String description, String code,
            int expiresInMinutes, String details) {
        publish(new SecurityEmailRequest(email, subject, title, description, code, expiresInMinutes, details));
    }

    public void sendNotice(String email, String subject, String title, String description, String details) {
        publish(new SecurityEmailRequest(email, subject, title, description, null, null, details));
    }

    public void sendDataExport(String email, String name, String json) {
        securityEmailService.sendDataExport(email, name, json);
    }

    private void publish(SecurityEmailRequest request) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.SECURITY_ROUTING_KEY, request);
    }
}
