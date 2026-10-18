package com.unisal.selectaapi.vagas.event;

import java.util.UUID;

/** Publicado quando uma vaga é criada ou editada. */
public record VagaSalvaEvent(UUID vagaId) {
}
