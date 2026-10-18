package com.unisal.selectaapi.chat.controler;

import com.unisal.selectaapi.chat.dtos.CreateMessageDto;
import com.unisal.selectaapi.chat.dtos.HistoricoMessageResponseDto;
import com.unisal.selectaapi.chat.service.HistoricoMensagensService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/message")
public class HistoricoMensagensControler {

    private final HistoricoMensagensService historicoMensagensService;

    public HistoricoMensagensControler(HistoricoMensagensService historicoMensagensService) {
        this.historicoMensagensService = historicoMensagensService;
    }

    @GetMapping("/{candidaturaId}")
    public ResponseEntity<List<HistoricoMessageResponseDto>> getMessages(@AuthenticationPrincipal Jwt jwt,
                                                                         @PathVariable("candidaturaId") UUID candidaturaId){
        var userId = UUID.fromString(jwt.getSubject());
        var lista = historicoMensagensService.pegarListaDeMensagens(userId, candidaturaId);
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{candidaturaId}")
    public ResponseEntity<HistoricoMessageResponseDto> registerMessage(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable("candidaturaId") UUID candidaturaId,
                                                    @Valid @RequestBody CreateMessageDto dto){
        var userId = UUID.fromString(jwt.getSubject());
        var mensagem = historicoMensagensService.registrarMensagem(dto, userId, candidaturaId);
        return ResponseEntity.created(URI.create("/message/" + candidaturaId)).body(mensagem);
    }

    @PatchMapping("/{candidaturaId}/lidas")
    public ResponseEntity<Void> marcarMensagensComoLidas(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable("candidaturaId") UUID candidaturaId){
        var userId = UUID.fromString(jwt.getSubject());
        historicoMensagensService.marcarMensagensComoLidas(userId, candidaturaId, Instant.now());
        return ResponseEntity.noContent().build();
    }


}
