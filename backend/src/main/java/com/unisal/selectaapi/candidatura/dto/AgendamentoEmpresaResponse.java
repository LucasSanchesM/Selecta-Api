package com.unisal.selectaapi.candidatura.dto;

import java.time.Instant;
import java.util.UUID;

import com.unisal.selectaapi.candidatura.entity.StatusAgendamento;
import com.unisal.selectaapi.candidatura.entity.StatusCandidatura;

/**
 * Agendamento visto pela empresa na listagem geral ("Meus agendamentos").
 * Além dos dados da reunião, traz o contexto necessário para exibir a linha
 * sem uma segunda consulta: quem é o candidato e para qual vaga é a reunião.
 * E-mail e foto do candidato respeitam as preferências de privacidade dele.
 */
public record AgendamentoEmpresaResponse(
        UUID id,
        UUID candidaturaId,
        StatusCandidatura candidaturaStatus,
        UUID vagaId,
        String vagaTitulo,
        UUID candidatoUserId,
        String candidatoNome,
        String candidatoEmail,
        String candidatoHeadline,
        String candidatoImagemUrl,
        UUID etapaId,
        String etapaDescricao,
        String titulo,
        String mensagem,
        Instant inicio,
        Integer duracaoMinutos,
        String link,
        StatusAgendamento status,
        String statusDescricao,
        UUID criadoPorUserId,
        String criadoPorNome,
        String motivoCancelamento,
        Instant createdAt,
        Instant updatedAt
) {
}
