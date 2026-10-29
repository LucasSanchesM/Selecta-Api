package com.unisal.selectaapi.vagas.dto;

import java.time.Instant;
import java.util.UUID;

public record TipoDeEtapaResponseDto(UUID id_etapa, String nome, UUID company_id, boolean isSystem, boolean em_uso,  boolean disponibilidade, Instant created_at) {
}
