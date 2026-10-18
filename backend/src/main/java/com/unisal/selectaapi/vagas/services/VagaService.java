package com.unisal.selectaapi.vagas.services;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.company.repository.CompanyProfileRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.geo.GeoCodingService;
import com.unisal.selectaapi.recomendacao.service.RecomendacaoService;
import com.unisal.selectaapi.vagas.VagaRole;
import com.unisal.selectaapi.vagas.VagaSpecification;
import com.unisal.selectaapi.vagas.dto.CreateVagaDto;
import com.unisal.selectaapi.vagas.dto.EtapaVagaResponse;
import com.unisal.selectaapi.vagas.dto.UpdateVaga;
import com.unisal.selectaapi.vagas.dto.VagaResponse;
import com.unisal.selectaapi.vagas.entity.ModalidadeVaga;
import com.unisal.selectaapi.vagas.entity.Vaga;
import com.unisal.selectaapi.vagas.entity.VagaEtapa;
import com.unisal.selectaapi.vagas.event.VagaRemovidaEvent;
import com.unisal.selectaapi.vagas.event.VagaSalvaEvent;
import com.unisal.selectaapi.vagas.repository.VagaRepository;

@Service
public class VagaService {

    private final VagaRepository vagaRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final GeoCodingService geoCodingService;
    private final RecomendacaoService recomendacaoService;
    private final ApplicationEventPublisher eventPublisher;

    public VagaService(
            VagaRepository vagaRepository,
            CompanyProfileRepository companyProfileRepository,
            GeoCodingService geoCodingService,
            RecomendacaoService recomendacaoService,
            ApplicationEventPublisher eventPublisher) {

        this.vagaRepository = vagaRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.geoCodingService = geoCodingService;
        this.recomendacaoService = recomendacaoService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public VagaResponse createVaga(CreateVagaDto dto) {

        CompanyProfile empresa = companyProfileRepository
                .findById(dto.companyProfileId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Empresa não encontrada com o id: "
                                + dto.companyProfileId()));

        var dtoAdress = geoCodingService.retornarEndereco(
                dto.latitude(),
                dto.longitude());

        var vaga = new Vaga();
        vaga.setTitulo(dto.titulo());
        vaga.setSalario(dto.salario());
        vaga.setDescricao(dto.descricao());
        vaga.setBeneficios(dto.beneficios());
        vaga.setEmpresa(empresa);
        vaga.setCidade(dtoAdress.cidade());
        vaga.setEstado(dtoAdress.estado());
        vaga.setLongitude(dto.longitude());
        vaga.setLatitude(dto.latitude());
        vaga.setCategoria(dto.categoria());
        vaga.setModalidade(dto.modalidade());
        vaga.setAtiva(true);

        configurarEtapas(vaga, dto.etapas());

        var savedVaga = vagaRepository.save(vaga);

        eventPublisher.publishEvent(
                new VagaSalvaEvent(savedVaga.getId()));

        return mapToResponse(savedVaga);
    }

    @Transactional(readOnly = true)
    public List<VagaResponse> findAllWithFilters(
            String titulo,
            VagaRole categoria,
            ModalidadeVaga modalidade,
            BigDecimal salarioMin,
            BigDecimal salarioMax,
            String cidade,
            String estado,
            Boolean ativa,
            UUID companyProfileId,
            UUID usuarioId,
            int page,
            int size) {

        var spec = VagaSpecification.withFilters(
                titulo,
                categoria,
                modalidade,
                salarioMin,
                salarioMax,
                cidade,
                estado,
                ativa,
                companyProfileId);

        int pagina = Math.max(page, 0);
        int tamanho = Math.min(Math.max(size, 1), 50);

        var maisRecentes = Sort.by(
                Sort.Direction.DESC,
                "createdAt");

        // Listagem por empresa é a visão do recrutador:
        // fica sempre em ordem cronológica.
        var vetorCandidato = companyProfileId == null
                ? recomendacaoService.vetorDoCandidato(usuarioId)
                : Optional.<float[]>empty();

        if (vetorCandidato.isPresent()) {
            var filtradas = vagaRepository.findAll(spec, maisRecentes);

            return recomendacaoService
                    .ordenarPorRelevancia(vetorCandidato.get(), filtradas)
                    .stream()
                    .skip((long) pagina * tamanho)
                    .limit(tamanho)
                    .map(this::mapToResponse)
                    .toList();
        }

        return vagaRepository
                .findAll(
                        spec,
                        PageRequest.of(pagina, tamanho, maisRecentes))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<VagaResponse> findAllByCompany(UUID companyProfileId) {

        companyProfileRepository.findById(companyProfileId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Empresa não encontrada com o id: "
                                + companyProfileId));

        return vagaRepository.findByEmpresaId(companyProfileId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public VagaResponse findVagaById(UUID id) {

        var vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Vaga não encontrada"));

        return mapToResponse(vaga);
    }

    @Transactional
    public VagaResponse updateVaga(UUID id, UpdateVaga dto) {

        var vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Vaga não encontrada"));

        if (dto.titulo() != null && !dto.titulo().isBlank()) {
            vaga.setTitulo(dto.titulo());
        }

        if (dto.salario() != null) {
            vaga.setSalario(dto.salario());
        }

        if (dto.descricao() != null && !dto.descricao().isBlank()) {
            vaga.setDescricao(dto.descricao());
        }

        if (dto.beneficios() != null && !dto.beneficios().isBlank()) {
            vaga.setBeneficios(dto.beneficios());
        }

        if (dto.latitude() != null) {
            vaga.setLatitude(dto.latitude());
        }

        if (dto.longitude() != null) {
            vaga.setLongitude(dto.longitude());
        }

        if (dto.categoria() != null) {
            vaga.setCategoria(dto.categoria());
        }

        if (dto.modalidade() != null) {
            vaga.setModalidade(dto.modalidade());
        }

        if (dto.ativa() != null) {
            vaga.setAtiva(dto.ativa());
        }

        if (dto.latitude() != null || dto.longitude() != null) {
            var enderecoNovo = geoCodingService.retornarEndereco(
                    vaga.getLatitude(),
                    vaga.getLongitude());

            vaga.setCidade(enderecoNovo.cidade());
            vaga.setEstado(enderecoNovo.estado());
        }

        var updatedVaga = vagaRepository.save(vaga);

        eventPublisher.publishEvent(
                new VagaSalvaEvent(updatedVaga.getId()));

        return mapToResponse(updatedVaga);
    }

    @Transactional
    public void deleteVaga(UUID id) {

        var vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Vaga não encontrada"));

        vagaRepository.delete(vaga);

        eventPublisher.publishEvent(new VagaRemovidaEvent(id));
    }

    public List<VagaResponse> pegarVagasPorRaio(
            Double latitude,
            Double longitude,
            Double raioKm) {

        var listaVagas = vagaRepository.listarVagasPorRaio(
                latitude,
                longitude,
                raioKm);

        return listaVagas.stream()
                .map(vaga -> mapToResponse(vaga))
                .toList();
    }

    public VagaResponse mapToResponse(Vaga vaga) {

        CompanyProfile empresa = vaga.getEmpresa();
        List<VagaEtapa> etapas = vaga.getEtapas();

        List<EtapaVagaResponse> etapasResponse = etapas.isEmpty()
                ? EtapaProcesso.etapasConfiguraveisPadrao()
                        .stream()
                        .map(etapa -> new EtapaVagaResponse(
                                etapa,
                                etapa.getDescricao(),
                                EtapaProcesso.etapasConfiguraveisPadrao()
                                        .indexOf(etapa) + 1))
                        .toList()
                : etapas.stream()
                        .map(etapa -> new EtapaVagaResponse(
                                etapa.getEtapa(),
                                etapa.getEtapa().getDescricao(),
                                etapa.getOrdem()))
                        .toList();

        return new VagaResponse(
                vaga.getId(),
                vaga.getTitulo(),
                vaga.getSalario(),
                vaga.getDescricao(),
                vaga.getBeneficios(),
                empresa.getId(),
                empresa.getCompanyName(),
                empresa.getCnpj(),
                empresa.getLogoUrl(),
                vaga.getCidade(),
                vaga.getEstado(),
                vaga.getLatitude(),
                vaga.getLongitude(),
                vaga.getCategoria(),
                vaga.getModalidade(),
                etapasResponse,
                vaga.getAtiva(),
                vaga.getCreatedAt(),
                vaga.getUpdatedAt());
    }

    private void configurarEtapas(
            Vaga vaga,
            List<EtapaProcesso> etapasInformadas) {

        List<EtapaProcesso> etapas =
                etapasInformadas == null || etapasInformadas.isEmpty()
                        ? EtapaProcesso.etapasConfiguraveisPadrao()
                        : etapasInformadas;

        if (etapas.stream().anyMatch(etapa -> etapa == null)
                || etapas.contains(EtapaProcesso.INSCRICAO)
                || etapas.contains(EtapaProcesso.CONTRATACAO)
                || new HashSet<>(etapas).size() != etapas.size()) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "A vaga deve possuir etapas configuráveis distintas, "
                            + "sem INSCRICAO ou CONTRATACAO");
        }

        vaga.setEtapas(
                etapas.stream()
                        .map(etapa -> new VagaEtapa(
                                vaga,
                                etapa,
                                etapas.indexOf(etapa) + 1))
                        .toList());
    }
}