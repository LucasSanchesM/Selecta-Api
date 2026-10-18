package com.unisal.selectaapi.notificacoes.controler;


import com.unisal.selectaapi.notificacoes.dtos.NotificacaoResponseDto;
import com.unisal.selectaapi.notificacoes.service.NotificacaoService;
import jakarta.persistence.PrePersist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoControler {

    private final NotificacaoService notificacaoService;

    public NotificacaoControler(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @PatchMapping("/{notificacaoId}")
    public ResponseEntity<Void> setNotificationAsReadByIdd(@PathVariable("notificacaoId") UUID notificacaoId,
                                                                                @AuthenticationPrincipal Jwt jwt){
        notificacaoService.marcarNotificacaoComoLidaPeloId(notificacaoId, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok().build();
    }

    @PatchMapping
    public ResponseEntity<Void> setAllNotificationsAsRead(@AuthenticationPrincipal Jwt jwt){
        notificacaoService.marcarTodasNotificacoesComoLidas(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<NotificacaoResponseDto>> getAllUnreadNotifications(@AuthenticationPrincipal Jwt jwt,
                                                                                  @PageableDefault(size = 20) Pageable pageable){
        return ResponseEntity.ok(
                        notificacaoService.pegarNotificacoesNaoLidas(UUID.fromString(jwt.getSubject()), pageable)
                    );
    }

    @GetMapping("/unreads")
    public ResponseEntity<Long> getHowMuchUnreadNotificationExist(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(notificacaoService.pegarQuantidadeDeNotificacoesNaoLidas(UUID.fromString(jwt.getSubject())));
    }

}
