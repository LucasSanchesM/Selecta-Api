package com.unisal.selectaapi.vagas.services;

import com.unisal.selectaapi.company.service.CompanyMemberService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.vagas.dto.CreateEtapaDto;
import com.unisal.selectaapi.vagas.dto.TipoDeEtapaResponseDto;
import com.unisal.selectaapi.vagas.dto.UpdateTipoDeEtapaDto;
import com.unisal.selectaapi.vagas.entity.TipoDeEtapa;
import com.unisal.selectaapi.vagas.repository.TipoDeEtapaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TipoDeEtapaService {
    private final TipoDeEtapaRepository tipoDeEtapaRepository;
    private final CompanyMemberService companyMemberService;
    private static final List<String> ORDEM_ETAPAS_SISTEMA = List.of(
            "TRIAGEM", "ENTREVISTA_RH", "TESTE_TECNICO", "ENTREVISTA_TECNICA", "PROPOSTA"
    );
    public static final String ETAPA_INICIAL = "INSCRICAO";
    public static final String ETAPA_FINAL = "ENCERRADA";

    public TipoDeEtapaService(TipoDeEtapaRepository tipoDeEtapaRepository, CompanyMemberService companyMemberService) {
        this.tipoDeEtapaRepository = tipoDeEtapaRepository;
        this.companyMemberService = companyMemberService;
    }

    @Transactional
    public TipoDeEtapaResponseDto criarEtapa(UUID company_user_Id, CreateEtapaDto createEtapaDto){
        var company = companyMemberService.pegarCompanyPeloUserId(company_user_Id);
        if(tipoDeEtapaRepository.existsByCompanyProfileIdAndNomeIgnoreCase(company.getId(), createEtapaDto.nomeEtapa().trim())){
            throw new BusinessException(HttpStatus.CONFLICT, "Uma etapa com este nome ja existe");
        }
        var etapa = new TipoDeEtapa();
        etapa.setNome(createEtapaDto.nomeEtapa().trim());
        etapa.setCompanyProfile(company);
        var saved = tipoDeEtapaRepository.save(etapa);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TipoDeEtapaResponseDto> listarEtapasDaEmpresa(UUID company_user_id){
        var company = companyMemberService.pegarCompanyPeloUserId(company_user_id);
        var listaBruta = tipoDeEtapaRepository.findAllByCompanyProfile_Id(company.getId());
        return listaBruta.stream()
                        .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TipoDeEtapaResponseDto> listarEtapasDoSistema() {
        var etapas = tipoDeEtapaRepository.findAllSystemEtapas();

        var ordem = new java.util.HashMap<String, Integer>();
        ordem.put(ETAPA_INICIAL, -1);

        for (int i = 0; i < ORDEM_ETAPAS_SISTEMA.size(); i++) {
            ordem.put(ORDEM_ETAPAS_SISTEMA.get(i), i);
        }

        ordem.put(ETAPA_FINAL, ORDEM_ETAPAS_SISTEMA.size());

        return etapas.stream()
                .sorted(java.util.Comparator.comparingInt(
                        etapa -> ordem.getOrDefault(etapa.getCodigo(), Integer.MAX_VALUE)
                ))
                .map(this::toDto)
                .toList();
    }


    @Transactional(readOnly = true)
    public TipoDeEtapaResponseDto pegarEtapaPeloId(UUID etapaId, UUID company_user_id){
        var company = companyMemberService.pegarCompanyPeloUserId(company_user_id);
        var etapa = tipoDeEtapaRepository.findById(etapaId)
                            .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Esta Etapa não existe"));
        if(etapa.getCompanyProfile() != null && !etapa.getCompanyProfile().getId().equals(company.getId())){
            throw new BusinessException(HttpStatus.NOT_FOUND, "Não existe esta etapa nos registros");
        }
        return toDto(etapa);
    }

    @Transactional
    public TipoDeEtapaResponseDto updateEtapaById(UUID company_user_id, UUID etapaId,  UpdateTipoDeEtapaDto dto){
        var company = companyMemberService.pegarCompanyPeloUserId(company_user_id);
        var etapa = tipoDeEtapaRepository.findById(etapaId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Esta Etapa não existe"));
        if(etapa.isSystem()){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Etapas do Sistema não podem ser alteradas");
        }
        if(!etapa.getCompanyProfile().getId().equals(company.getId())){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Esta etapa não existe para o seu usuario");
        }
        if(dto.name() != null && !dto.name().isBlank()){
            var novoNome = dto.name().trim();
            if(!etapa.getNome().equalsIgnoreCase(novoNome) && tipoDeEtapaRepository.existsByCompanyProfileIdAndNomeIgnoreCase(company.getId(), novoNome)){
                throw new BusinessException(HttpStatus.CONFLICT, "Uma etapa com este nome ja existe");
            }
            etapa.setNome(novoNome);
        }
        return  toDto(etapa);
    }

    @Transactional(readOnly = true)
    public List<TipoDeEtapaResponseDto> listarEtapasDisponiveis(UUID company_user_id){
        var companyId = companyMemberService.pegarCompanyPeloUserId(company_user_id).getId();
        var lista = tipoDeEtapaRepository.findEtapasDisponiveisParaEmpresa(companyId);
        return lista.stream().map(this::toDto).toList();
    }

    @Transactional
    public TipoDeEtapaResponseDto alterarEstadoEtapa(UUID etapaId, UUID company_user_id, boolean novoEstado){
        var companyId = companyMemberService.pegarCompanyPeloUserId(company_user_id).getId();
        var etapa = tipoDeEtapaRepository.findById(etapaId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Esta Etapa não existe"));
        if(etapa.isSystem()){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Etapas do sistema não podem ser alteradas");
        }
        if(etapa.getCompanyProfile() != null && !etapa.getCompanyProfile().getId().equals(companyId)){
            throw new BusinessException(HttpStatus.NOT_FOUND, "Não existe esta etapa nos registros");
        }
        if(etapa.isDisponibilidade() == novoEstado && !novoEstado){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Esta etapa já está desabilitada");
        }
        if(etapa.isDisponibilidade() == novoEstado && novoEstado){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Esta etapa já está habilitada");
        }
        etapa.setDisponibilidade(novoEstado);
        return toDto(etapa);
    }

    @Transactional
    public void deletarTipoDeEtapa(UUID etapaId, UUID company_user_id){
        var companyId = companyMemberService.pegarCompanyPeloUserId(company_user_id).getId();
        var etapa = tipoDeEtapaRepository.findById(etapaId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Esta Etapa não existe"));
        if(etapa.isSystem()){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Etapas do sistema não podem ser deletadas");
        }
        if(etapa.getCompanyProfile() != null && !etapa.getCompanyProfile().getId().equals(companyId)){
            throw new BusinessException(HttpStatus.NOT_FOUND, "Não existe esta etapa nos registros");
        }
        if(etapa.isJaUtilizada()){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Etapas já utilizadas não podem ser deletadas");
        }
        tipoDeEtapaRepository.delete(etapa);
    }

    @Transactional
    public void marcarEtapaComoJaUtilizada(UUID etapaId, UUID company_user_id){
        var companyId = companyMemberService.pegarCompanyPeloUserId(company_user_id).getId();
        var etapa = tipoDeEtapaRepository.findById(etapaId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Esta Etapa não existe"));
        if(etapa.getCompanyProfile() != null && !etapa.getCompanyProfile().getId().equals(companyId)){
            throw new BusinessException(HttpStatus.NOT_FOUND, "Não existe esta etapa nos registros");
        }
        if(!etapa.isDisponibilidade()){
            throw new BusinessException(HttpStatus.CONFLICT, "Esta etapa está desabilitada");
        }
        etapa.marcarComoUtilizada();
    }

    private TipoDeEtapaResponseDto toDto(TipoDeEtapa etapa){
        return new TipoDeEtapaResponseDto(  etapa.getId(),
                                            etapa.getNome(),
                                            etapa.isSystem() ? null :etapa.getCompanyProfile().getId(),
                                            etapa.isSystem(),
                                            etapa.isJaUtilizada(),
                                            etapa.isDisponibilidade(),
                                            etapa.isSystem() ? null : etapa.getCreated_at()
                );
    }
}
