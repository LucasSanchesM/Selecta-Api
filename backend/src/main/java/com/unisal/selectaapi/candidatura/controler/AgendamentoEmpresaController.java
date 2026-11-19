package com.unisal.selectaapi.candidatura.controler;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unisal.selectaapi.candidatura.dto.AgendamentoEmpresaResponse;
import com.unisal.selectaapi.candidatura.entity.StatusAgendamento;
import com.unisal.selectaapi.candidatura.service.CandidaturaAgendamentoService;
import com.unisal.selectaapi.company.service.CompanyMemberService;

/**
 * Visão geral dos agendamentos da empresa ("Meus agendamentos").
 * As rotas por candidatura (criar, editar, cancelar) continuam em
 * {@link CandidaturaEmpresaController}; aqui fica apenas a listagem transversal.
 * Candidatos não possuem empresa associada, então são recusados (404) ao resolver a empresa.
 */
@RestController
@RequestMapping("/empresa/agendamentos")
public class AgendamentoEmpresaController {

    private final CandidaturaAgendamentoService agendamentoService;
    private final CompanyMemberService companyMemberService;

    public AgendamentoEmpresaController(CandidaturaAgendamentoService agendamentoService,
                                        CompanyMemberService companyMemberService) {
        this.agendamentoService = agendamentoService;
        this.companyMemberService = companyMemberService;
    }

    // LISTAR AGENDAMENTOS DA EMPRESA (com filtros opcionais)
    @GetMapping
    public ResponseEntity<List<AgendamentoEmpresaResponse>> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false, defaultValue = "false") boolean criadoPorMim,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate) {

        UUID userId = UUID.fromString(jwt.getSubject());
        UUID empresaId = companyMemberService.pegarCompanyPeloUserId(userId).getId();

        return ResponseEntity.ok(agendamentoService.listarDaEmpresa(
                empresaId, criadoPorMim ? userId : null, status, de, ate));
    }
}
