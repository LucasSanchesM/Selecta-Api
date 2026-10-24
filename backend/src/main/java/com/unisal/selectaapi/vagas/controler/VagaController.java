package com.unisal.selectaapi.vagas.controler;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


import com.unisal.selectaapi.vagas.VagaRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unisal.selectaapi.vagas.dto.UpdateVaga;
import com.unisal.selectaapi.vagas.dto.VagaResponse;
import com.unisal.selectaapi.vagas.entity.ModalidadeVaga;
import com.unisal.selectaapi.vagas.services.VagaService;
import com.unisal.selectaapi.vagas.dto.CreateVagaDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/vagas")
public class VagaController {


    private final VagaService vagaService;

    public VagaController(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    // CRIAR UMA VAGA
    @PostMapping
    public ResponseEntity<VagaResponse> createVaga(
            @Valid @RequestBody CreateVagaDto dto) {

        var vaga = vagaService.createVaga(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(vaga);
    }

    // LISTAR TODAS AS VAGAS COM FILTROS OPCIONAIS
    @GetMapping
    public ResponseEntity<List<VagaResponse>> findAllVagas(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) VagaRole categoria,
            @RequestParam(required = false) ModalidadeVaga modalidade,
            @RequestParam(required = false) BigDecimal salarioMin,
            @RequestParam(required = false) BigDecimal salarioMax,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Boolean ativa,
            @RequestParam(required = false) UUID companyProfileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal Jwt jwt) {

        var listagem = vagaService.listarVagas(
                titulo, categoria, modalidade,
                salarioMin, salarioMax, cidade, estado, ativa, companyProfileId,
                page, size, usuarioId(jwt)
        );

        return ResponseEntity.ok()
                .header("X-Recomendacao", listagem.personalizada() ? "personalizada" : "padrao")
                .body(listagem.vagas());
    }

    private static UUID usuarioId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return null;
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
    // LISTAR VAGAS DE UMA EMPRESA ESPECÍFICA
    @GetMapping("/empresa/{companyProfileId}")
    public ResponseEntity<List<VagaResponse>> findVagasByCompany(
            @PathVariable UUID companyProfileId) {

        var vagas = vagaService.findAllByCompany(companyProfileId);

        return ResponseEntity.ok(vagas);
    }

    // BUSCAR UMA VAGA PELO ID
    @GetMapping("/{id}")
    public ResponseEntity<VagaResponse> findVagaById(
            @PathVariable UUID id) {

        var vaga = vagaService.findVagaById(id);

        return ResponseEntity.ok(vaga);
    }

    // ATUALIZAR UMA VAGA
    @PutMapping("/{id}")
    public ResponseEntity<VagaResponse> updateVaga(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVaga dto) {

        var vaga = vagaService.updateVaga(id, dto);

        return ResponseEntity.ok(vaga);
    }

    // DELETAR UMA VAGA
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVaga(
            @PathVariable UUID id) {

        vagaService.deleteVaga(id);

        return ResponseEntity.noContent().build();
    }
    //ver vags de acordo com distancia
    @GetMapping("/proximas")
    public ResponseEntity<List<VagaResponse>> listarvagasPorKm( @RequestParam @NotNull Double latitude,
                                                                @RequestParam @NotNull Double longitude,
                                                                @RequestParam @NotNull Double raioKm) {
        return ResponseEntity.ok(vagaService.pegarVagasPorRaio(latitude, longitude, raioKm));
    }
    
}
