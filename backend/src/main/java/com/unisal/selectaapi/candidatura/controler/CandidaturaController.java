package com.unisal.selectaapi.candidatura.controler;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unisal.selectaapi.candidatura.dto.CandidaturaDetalheResponse;
import com.unisal.selectaapi.candidatura.dto.CandidaturaResponse;
import com.unisal.selectaapi.candidatura.dto.AgendamentoResponse;
import com.unisal.selectaapi.candidatura.dto.CreateCandidatura;
import com.unisal.selectaapi.candidatura.dto.FeedbackResponse;
import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.candidatura.service.CandidaturaFeedbackService;
import com.unisal.selectaapi.candidatura.service.CandidaturaAgendamentoService;
import com.unisal.selectaapi.candidatura.service.CandidaturaService;
import jakarta.validation.Valid;

/**
 * Rotas do candidato: se candidatar, acompanhar e ler feedbacks.
 */
@RestController
@RequestMapping("/candidaturas")
public class CandidaturaController {

    private final CandidaturaService candidaturaService;
    private final CandidaturaFeedbackService feedbackService;
    private final CandidaturaAgendamentoService agendamentoService;

    public CandidaturaController(CandidaturaService candidaturaService,
            CandidaturaFeedbackService feedbackService,
            CandidaturaAgendamentoService agendamentoService) {
        this.candidaturaService = candidaturaService;
        this.feedbackService = feedbackService;
        this.agendamentoService = agendamentoService;
    }

    // SE CANDIDATAR A UMA VAGA
    @PostMapping
    public ResponseEntity<CandidaturaResponse> candidatar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCandidatura dto) {

        UUID userId = UUID.fromString(jwt.getSubject());
        var candidatura = candidaturaService.candidatar(userId, dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(candidatura);
    }

    // LISTAR AS MINHAS CANDIDATURAS
    @GetMapping("/me")
    public ResponseEntity<List<CandidaturaResponse>> listarMinhasCandidaturas(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(candidaturaService.listarMinhasCandidaturas(userId));
    }

    // ACOMPANHAR UMA CANDIDATURA (timeline + feedbacks)
    @GetMapping("/{id}")
    public ResponseEntity<CandidaturaDetalheResponse> detalhar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(candidaturaService.detalharComoCandidato(userId, id));
    }

    // LISTAR OS FEEDBACKS RECEBIDOS (opcionalmente de uma etapa específica)
    @GetMapping("/{id}/feedbacks")
    public ResponseEntity<List<FeedbackResponse>> listarFeedbacks(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID etapaId) {

        UUID userId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(feedbackService.listarComoCandidato(userId, id, etapaId));
    }

    @GetMapping("/{id}/agendamentos")
    public ResponseEntity<List<AgendamentoResponse>> listarAgendamentos(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(agendamentoService.listarComoCandidato(userId, id));
    }

    @PatchMapping("/{id}/agendamentos/{agendamentoId}/confirmar")
    public ResponseEntity<AgendamentoResponse> confirmarAgendamento(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID agendamentoId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(agendamentoService.confirmar(userId, id, agendamentoId));
    }

    @PatchMapping("/{id}/agendamentos/{agendamentoId}/recusar")
    public ResponseEntity<AgendamentoResponse> recusarAgendamento(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID agendamentoId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(agendamentoService.recusar(userId, id, agendamentoId));
    }

    // DESISTIR DO PROCESSO
    @DeleteMapping("/{id}")
    public ResponseEntity<CandidaturaResponse> cancelar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(candidaturaService.cancelar(userId, id));
    }
}
