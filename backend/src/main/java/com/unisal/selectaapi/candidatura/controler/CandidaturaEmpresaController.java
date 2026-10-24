package com.unisal.selectaapi.candidatura.controler;

import java.util.List;
import java.util.UUID;

import com.unisal.selectaapi.company.service.CompanyMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unisal.selectaapi.candidatura.dto.AvancarEtapaDto;
import com.unisal.selectaapi.candidatura.dto.AgendamentoResponse;
import com.unisal.selectaapi.candidatura.dto.CandidaturaDetalheResponse;
import com.unisal.selectaapi.candidatura.dto.CandidaturaResponse;
import com.unisal.selectaapi.candidatura.dto.CreateAgendamentoDto;
import com.unisal.selectaapi.candidatura.dto.CreateFeedbackDto;
import com.unisal.selectaapi.candidatura.dto.DecidirCandidaturaDto;
import com.unisal.selectaapi.candidatura.dto.FeedbackResponse;
import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.candidatura.entity.StatusCandidatura;
import com.unisal.selectaapi.candidatura.service.CandidaturaFeedbackService;
import com.unisal.selectaapi.candidatura.service.CandidaturaAgendamentoService;
import com.unisal.selectaapi.candidatura.service.CandidaturaService;
import jakarta.validation.Valid;

/**
 * Rotas da empresa: gerenciar o funil das próprias vagas e escrever feedbacks.
 * Todas as consultas são filtradas pelo usuário logado, então uma empresa nunca
 * alcança candidaturas de vagas de outra.
 */
@RestController
@RequestMapping("/empresa/candidaturas")
public class CandidaturaEmpresaController {

    private final CandidaturaService candidaturaService;
    private final CandidaturaFeedbackService feedbackService;
    private final CandidaturaAgendamentoService agendamentoService;
    private final CompanyMemberService companyMemberService;

    public CandidaturaEmpresaController(CandidaturaService candidaturaService,
                                        CandidaturaFeedbackService feedbackService,
                                        CandidaturaAgendamentoService agendamentoService, CompanyMemberService companyMemberService) {
        this.candidaturaService = candidaturaService;
        this.feedbackService = feedbackService;
        this.agendamentoService = agendamentoService;
        this.companyMemberService = companyMemberService;
    }

    // LISTAR CANDIDATURAS DAS VAGAS DA EMPRESA (com filtros opcionais)
    @GetMapping
    public ResponseEntity<List<CandidaturaResponse>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID vagaId,
            @RequestParam(required = false) StatusCandidatura status,
            @RequestParam(required = false) EtapaProcesso etapa,
            @RequestParam(required = false) String candidatoNome) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getId();

        var candidaturas = candidaturaService.listarCandidaturasDaEmpresa(
                empresaUserId, vagaId, status, etapa, candidatoNome);

        return ResponseEntity.ok(candidaturas);
    }

    // VER UMA CANDIDATURA COMPLETA
    @GetMapping("/{id}")
    public ResponseEntity<CandidaturaDetalheResponse> detalhar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();

        return ResponseEntity.ok(candidaturaService.detalharComoEmpresa(empresaUserId, id));
    }

    // AVANÇAR O CANDIDATO DE ETAPA
    @PatchMapping("/{id}/etapa")
    public ResponseEntity<CandidaturaResponse> avancarEtapa(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody AvancarEtapaDto dto) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();

        return ResponseEntity.ok(candidaturaService.avancarEtapa(empresaUserId, id, dto));
    }

    // APROVAR OU REPROVAR A CANDIDATURA
    @PatchMapping("/{id}/status")
    public ResponseEntity<CandidaturaResponse> decidir(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody DecidirCandidaturaDto dto) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();

        return ResponseEntity.ok(candidaturaService.decidir(empresaUserId, id, dto));
    }

    // CRIAR FEEDBACK DE UMA ETAPA
    @PostMapping("/{id}/feedbacks")
    public ResponseEntity<FeedbackResponse> criarFeedback(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody CreateFeedbackDto dto) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();
        var feedback = feedbackService.criar(empresaUserId, id, dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }

    // LISTAR FEEDBACKS DA CANDIDATURA
    @GetMapping("/{id}/feedbacks")
    public ResponseEntity<List<FeedbackResponse>> listarFeedbacks(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(required = false) EtapaProcesso etapa) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();

        return ResponseEntity.ok(feedbackService.listarComoEmpresa(empresaUserId, id, etapa));
    }

    @PostMapping("/{id}/agendamentos")
    public ResponseEntity<AgendamentoResponse> criarAgendamento(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody CreateAgendamentoDto dto) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agendamentoService.criar(empresaUserId, id, dto));
    }

    @GetMapping("/{id}/agendamentos")
    public ResponseEntity<List<AgendamentoResponse>> listarAgendamentos(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();
        return ResponseEntity.ok(agendamentoService.listarComoEmpresa(empresaUserId, id));
    }

    @PutMapping("/{id}/agendamentos/{agendamentoId}")
    public ResponseEntity<AgendamentoResponse> atualizarAgendamento(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID agendamentoId,
            @Valid @RequestBody CreateAgendamentoDto dto) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();
        return ResponseEntity.ok(agendamentoService.atualizar(empresaUserId, id, agendamentoId, dto));
    }

    @DeleteMapping("/{id}/agendamentos/{agendamentoId}")
    public ResponseEntity<Void> cancelarAgendamento(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID agendamentoId,
            @RequestParam(required = false) String motivo) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();
        agendamentoService.cancelar(empresaUserId, id, agendamentoId, motivo);
        return ResponseEntity.noContent().build();
    }

    // EDITAR UM FEEDBACK
    @PutMapping("/{id}/feedbacks/{feedbackId}")
    public ResponseEntity<FeedbackResponse> atualizarFeedback(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID feedbackId,
            @Valid @RequestBody CreateFeedbackDto dto) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();

        return ResponseEntity.ok(feedbackService.atualizar(empresaUserId, id, feedbackId, dto));
    }

    // REMOVER UM FEEDBACK
    @DeleteMapping("/{id}/feedbacks/{feedbackId}")
    public ResponseEntity<Void> deletarFeedback(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID feedbackId) {

        UUID empresaUserId = companyMemberService.pegarCompanyPeloUserId(UUID.fromString(jwt.getSubject())).getUser().getId();
        feedbackService.deletar(empresaUserId, id, feedbackId);

        return ResponseEntity.noContent().build();
    }
}
