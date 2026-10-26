package com.unisal.selectaapi.vagas.dto;

import jakarta.validation.constraints.Size;


public record UpdateTipoDeEtapaDto(@Size(max = 100, message = "O nome da etapa deve ter no maximo 100 caracteres")String name) {
}
