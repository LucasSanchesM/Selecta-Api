package com.unisal.selectaapi.candidatura.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.candidatura.entity.StatusCandidatura;
import com.unisal.selectaapi.vagas.dto.EtapaVagaResponse;

public record CandidaturaResponse(

                UUID id,

                // Vaga
                UUID vagaId,
                String vagaTitulo,

                // Empresa
                UUID companyProfileId,
                String nomeEmpresa,
                String logoUrlEmpresa,

                // Candidato
                UUID candidateProfileId,
                UUID candidatoUserId,
                String candidatoNome,
                String candidatoEmail,
                String candidatoHeadline,
                String candidatoImagemUrl,

                // Situação do processo
                EtapaProcesso etapaAtual,
                String etapaAtualDescricao,
                List<EtapaVagaResponse> etapasVaga,
                StatusCandidatura status,
                String statusDescricao,

                String cartaApresentacao,
                String curriculoUrl,
                String motivoEncerramento,

                long totalFeedbacks,

                Instant finalizadaEm,
                Instant createdAt,
                Instant updatedAt

) {
}
