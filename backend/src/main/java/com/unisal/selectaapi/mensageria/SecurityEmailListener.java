package com.unisal.selectaapi.mensageria;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.unisal.selectaapi.config.RabbitMqConfig;
import com.unisal.selectaapi.mensageria.dto.SecurityEmailRequest;
import com.unisal.selectaapi.mensageria.service.SecurityEmailService;

@Component
public class SecurityEmailListener {

    private final SecurityEmailService securityEmailService;

    public SecurityEmailListener(SecurityEmailService securityEmailService) {
        this.securityEmailService = securityEmailService;
    }

    @RabbitListener(queues = RabbitMqConfig.SECURITY_QUEUE)
    public void messageListener(SecurityEmailRequest request) {
        securityEmailService.send(request);
    }
}
