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
    public List<TipoDeEtapaResponseDto> listarEtapasDoSistema(){
        return tipoDeEtapaRepository.findAllSystemEtapas().stream()
                                        .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TipoDeEtapaResponseDto pegarEtapaPeloId(UUID etapaId){
        var etapa = tipoDeEtapaRepository.findById(etapaId)
                            .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Esta Etapa não existe"));
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

    private TipoDeEtapaResponseDto toDto(TipoDeEtapa etapa){
        return new TipoDeEtapaResponseDto(  etapa.getId(),
                                            etapa.getNome(),
                                            etapa.isSystem() ? null :etapa.getCompanyProfile().getId(),
                                            etapa.isSystem());
    }
}
