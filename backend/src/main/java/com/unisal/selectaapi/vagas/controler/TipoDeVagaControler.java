package com.unisal.selectaapi.vagas.controler;

import com.unisal.selectaapi.vagas.dto.CreateEtapaDto;
import com.unisal.selectaapi.vagas.dto.TipoDeEtapaResponseDto;
import com.unisal.selectaapi.vagas.dto.UpdateTipoDeEtapaDto;
import com.unisal.selectaapi.vagas.services.TipoDeEtapaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/etapas")
public class TipoDeVagaControler {
    private final TipoDeEtapaService tipoDeEtapaService;

    public TipoDeVagaControler(TipoDeEtapaService tipoDeEtapaService) {
        this.tipoDeEtapaService = tipoDeEtapaService;
    }

    @PreAuthorize("hasAuthority('COMPANY')")
    @PostMapping
    public ResponseEntity<TipoDeEtapaResponseDto> createEtapa(@AuthenticationPrincipal Jwt jwt,
                                                              @RequestBody @Valid CreateEtapaDto dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoDeEtapaService.criarEtapa(pegarUserIdJwt(jwt), dto));
    }

    @PreAuthorize("hasAuthority('COMPANY')")
    @GetMapping
    public ResponseEntity<List<TipoDeEtapaResponseDto>> getAllAvailableEtapas(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(tipoDeEtapaService.listarEtapasDisponiveis(pegarUserIdJwt(jwt)));
    }

    @PreAuthorize("hasAuthority('COMPANY')")
    @GetMapping("/company")
    public ResponseEntity<List<TipoDeEtapaResponseDto>> getAllCompanyEtapas(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(tipoDeEtapaService.listarEtapasDaEmpresa(pegarUserIdJwt(jwt)));
    }

    @PreAuthorize("hasAuthority('COMPANY')")
    @GetMapping("/{id}")
    public ResponseEntity<TipoDeEtapaResponseDto> getEtapaByEtapaId(@PathVariable("id") UUID id,
                                                                    @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(tipoDeEtapaService.pegarEtapaPeloId(id, pegarUserIdJwt(jwt)));
    }

    @PreAuthorize("hasAuthority('COMPANY')")
    @GetMapping("/default")
    public ResponseEntity<List<TipoDeEtapaResponseDto>> getAllSystemEtapas(){
        return ResponseEntity.ok(tipoDeEtapaService.listarEtapasDoSistema());
    }

    @PreAuthorize("hasAuthority('COMPANY')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoDeEtapaResponseDto> updateEtapaById(@AuthenticationPrincipal Jwt jwt,
                                                                  @PathVariable("id") UUID id,
                                                                  @RequestBody @Valid UpdateTipoDeEtapaDto dto){
        return ResponseEntity.ok(tipoDeEtapaService.updateEtapaById(pegarUserIdJwt(jwt),id,dto));
    }

    @PreAuthorize("hasAuthority('COMPANY')")
    @PutMapping("/state/{id}")
    public ResponseEntity<TipoDeEtapaResponseDto> alterarEstadoEtapa(@AuthenticationPrincipal Jwt jwt,
                                                                  @PathVariable("id") UUID id,
                                                                  @RequestBody boolean novoEstado){
        return ResponseEntity.ok(tipoDeEtapaService.alterarEstadoEtapa(id, pegarUserIdJwt(jwt), novoEstado));
    }

    @PreAuthorize("hasAuthority('COMPANY')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarEstado(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable("id") UUID id){
        tipoDeEtapaService.deletarTipoDeEtapa(id, pegarUserIdJwt(jwt));
        return ResponseEntity.ok().build();
    }

    private UUID pegarUserIdJwt(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}
