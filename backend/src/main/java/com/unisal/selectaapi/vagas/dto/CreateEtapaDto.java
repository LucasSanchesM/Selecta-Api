package com.unisal.selectaapi.vagas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record CreateEtapaDto( @NotBlank(message = "Informe o nome da etapa") @Size(max = 100, message = "O nome da etapa deve ter no maximo 100 caracteres") String nomeEtapa){
}
