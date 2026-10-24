package com.unisal.selectaapi.vagas.dto;

import java.util.List;

public record ListagemVagas(List<VagaResponse> vagas, boolean personalizada) {
}
