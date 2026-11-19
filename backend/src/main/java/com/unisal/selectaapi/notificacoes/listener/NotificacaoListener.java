package com.unisal.selectaapi.notificacoes.listener;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificacaoListener {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public NotificacaoListener(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(CriarNotificacaoEvent e){
        simpMessagingTemplate.convertAndSendToUser(
                e.userId().toString(), "/queue/notificacoes", e.response());
    }
}