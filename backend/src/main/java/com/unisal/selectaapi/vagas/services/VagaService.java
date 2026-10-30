package com.unisal.selectaapi.vagas.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.unisal.selectaapi.vagas.entity.TipoDeEtapa;
import com.unisal.selectaapi.vagas.repository.TipoDeEtapaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.company.repository.CompanyProfileRepository;

import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.geo.GeoCodingService;
import com.unisal.selectaapi.recomendacao.EmbeddingRequestedEvent;
import com.unisal.selectaapi.recomendacao.FiltroVagas;
import com.unisal.selectaapi.recomendacao.VagaRecomendacaoService;
import com.unisal.selectaapi.vagas.repository.VagaRepository;
import com.unisal.selectaapi.vagas.VagaRole;
import com.unisal.selectaapi.vagas.VagaSpecification;
import com.unisal.selectaapi.vagas.dto.CreateVagaDto;
import com.unisal.selectaapi.vagas.dto.EtapaVagaResponse;
import com.unisal.selectaapi.vagas.dto.ListagemVagas;
import com.unisal.selectaapi.vagas.dto.UpdateVaga;
import com.unisal.selectaapi.vagas.dto.VagaResponse;
import com.unisal.selectaapi.vagas.entity.ModalidadeVaga;
import com.unisal.selectaapi.vagas.entity.Vaga;
import com.unisal.selectaapi.vagas.entity.VagaEtapa;

@Service
public class VagaService {

    private static final Logger log = LoggerFactory.getLogger(VagaService.class);

    private final VagaRepository vagaRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final GeoCodingService geoCodingService;
    private final ApplicationEventPublisher eventPublisher;
    private final VagaRecomendacaoService recomendacaoService;
    private final TransactionTemplate readOnlyTx;
    private final TipoDeEtapaRepository tipoDeEtapaRepository;

    public VagaService(VagaRepository vagaRepository, CompanyProfileRepository companyProfileRepository,
                       GeoCodingService geoCodingService, ApplicationEventPublisher eventPublisher,
                       VagaRecomendacaoService recomendacaoService, PlatformTransactionManager transactionManager, TipoDeEtapaRepository tipoDeEtapaRepository) {
        this.vagaRepository = vagaRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.geoCodingService = geoCodingService;
        this.eventPublisher = eventPublisher;
        this.recomendacaoService = recomendacaoService;
        this.tipoDeEtapaRepository = tipoDeEtapaRepository;
        if (transactionManager != null) {
            this.readOnlyTx = new TransactionTemplate(transactionManager);
            this.readOnlyTx.setReadOnly(true);
        } else {
            this.readOnlyTx = null;
        }
    }

    @Transactional
    public VagaResponse createVaga(CreateVagaDto dto) {

        CompanyProfile empresa = companyProfileRepository.findById(dto.companyProfileId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Empresa não encontrada com o id: " + dto.companyProfileId()
                ));

        var dtoAdress = geoCodingService.retornarEndereco(dto.latitude(), dto.longitude());
        
        var vaga = new Vaga();
        vaga.setTitulo(dto.titulo());
        vaga.setSalario(dto.salario());
        vaga.setQuantidadeVagas(dto.quantidadeVagas());
        vaga.setVagasPreenchidas(0);
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

        configurarEtapas(vaga, dto.etapasId());

        var savedVaga = vagaRepository.save(vaga);
        solicitarEmbedding(savedVaga);

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
            int page,
            int size
    ) {
        var spec = VagaSpecification.withFilters(
                titulo, categoria, modalidade,
                salarioMin, salarioMax, cidade, estado, ativa, companyProfileId
        );

        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return vagaRepository.findAll(spec, pageable)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Same listing as {@link #findAllWithFilters}, but when the caller is a candidate
     * with an indexed profile the page is ordered by semantic similarity.
     * Any failure in the recommendation path falls back to the default ordering.
     */
    public ListagemVagas listarVagas(
            String titulo,
            VagaRole categoria,
            ModalidadeVaga modalidade,
            BigDecimal salarioMin,
            BigDecimal salarioMax,
            String cidade,
            String estado,
            Boolean ativa,
            UUID companyProfileId,
            int page,
            int size,
            UUID usuarioId
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        if (usuarioId != null && recomendacaoService != null && readOnlyTx != null && recomendacaoService.isAtiva()) {
            try {
                var filtro = new FiltroVagas(titulo, categoria, modalidade, salarioMin, salarioMax,
                        cidade, estado, ativa, companyProfileId);
                var ids = recomendacaoService.rankearVagas(usuarioId, filtro, safePage, safeSize);
                if (ids.isPresent()) {
                    List<VagaResponse> vagas = readOnlyTx.execute(status -> carregarNaOrdem(ids.get()));
                    return new ListagemVagas(vagas, true);
                }
            } catch (DataAccessException exception) {
                log.warn("Recomendação indisponível, usando ordenação padrão: {}",
                        exception.getMostSpecificCause().getMessage());
            }
        }

        List<VagaResponse> vagas = readOnlyTx != null
                ? readOnlyTx.execute(status -> findAllWithFilters(titulo, categoria, modalidade,
                        salarioMin, salarioMax, cidade, estado, ativa, companyProfileId, safePage, safeSize))
                : findAllWithFilters(titulo, categoria, modalidade,
                        salarioMin, salarioMax, cidade, estado, ativa, companyProfileId, safePage, safeSize);
        return new ListagemVagas(vagas, false);
    }

    private List<VagaResponse> carregarNaOrdem(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Vaga> porId = vagaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Vaga::getId, Function.identity()));
        return ids.stream()
                .map(porId::get)
                .filter(java.util.Objects::nonNull)
                .map(this::mapToResponse)
                .toList();
    }

    private void solicitarEmbedding(Vaga vaga) {
        if (eventPublisher != null && vaga != null && vaga.getId() != null) {
            eventPublisher.publishEvent(EmbeddingRequestedEvent.vaga(vaga.getId()));
        }
    }

    public List<VagaResponse> findAllByCompany(UUID companyProfileId) {

        companyProfileRepository.findById(companyProfileId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Empresa não encontrada com o id: " + companyProfileId
                ));

        return vagaRepository.findByEmpresaId(companyProfileId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public VagaResponse findVagaById(UUID id) {

        var vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Vaga não encontrada"
                ));

        return mapToResponse(vaga);
    }

    @Transactional
    public VagaResponse updateVaga(UUID id, UpdateVaga dto) {

        var vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Vaga não encontrada"
                ));

        if (dto.titulo() != null && !dto.titulo().isBlank()) {
            vaga.setTitulo(dto.titulo());
        }

        if (dto.salario() != null) {
            vaga.setSalario(dto.salario());
        }

        if (dto.quantidadeVagas() != null) {
            int preenchidas = vaga.getVagasPreenchidas() == null ? 0 : vaga.getVagasPreenchidas();
            if (dto.quantidadeVagas() < preenchidas) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "A quantidade de vagas não pode ser menor que as já preenchidas (" + preenchidas + ")");
            }
            vaga.setQuantidadeVagas(dto.quantidadeVagas());
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

        if(dto.latitude() != null || dto.longitude() != null){
            var enderecoNovo = geoCodingService.retornarEndereco(vaga.getLatitude(), vaga.getLongitude());
            vaga.setCidade(enderecoNovo.cidade());
            vaga.setEstado(enderecoNovo.estado());
        }
        var updatedVaga = vagaRepository.save(vaga);
        solicitarEmbedding(updatedVaga);

        return mapToResponse(updatedVaga);
    }

    @Transactional
    public void deleteVaga(UUID id) {

        var vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Vaga não encontrada"
                ));

        vagaRepository.delete(vaga);
    }

    public List<VagaResponse> pegarVagasPorRaio(Double latitude, Double longitude, Double raioKm){    
        var listaVagas = vagaRepository.listarVagasPorRaio(latitude, longitude, raioKm);
        return listaVagas.stream().map(vaga -> mapToResponse(vaga)).toList();
    }

    private VagaResponse mapToResponse(Vaga vaga) {

        CompanyProfile empresa = vaga.getEmpresa();
        List<VagaEtapa> etapas = vaga.getEtapas();

        List<EtapaVagaResponse> etapasResponse = etapas.stream()
                .map(etapa -> new EtapaVagaResponse(
                        etapa.getEtapa().getId(),
                        etapa.getEtapa().getNome(),
                        etapa.getOrdem()
                ))
                .toList();

        return new VagaResponse(
                vaga.getId(),
                vaga.getTitulo(),
                vaga.getSalario(),
                vaga.getQuantidadeVagas(),
                vaga.getVagasPreenchidas(),
                vaga.getVagasDisponiveis(),
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
                vaga.getUpdatedAt()
        );
    }

    private void configurarEtapas(Vaga vaga, List<UUID> etapasId) {

        var etapaInicial = tipoDeEtapaRepository
                .findByCodigoAndCompanyProfileIsNull("INSCRICAO")
                .orElseThrow(() -> new IllegalStateException(
                        "Etapa inicial INSCRICAO não cadastrada"
                ));

        var etapaFinal = tipoDeEtapaRepository
                .findByCodigoAndCompanyProfileIsNull("ENCERRADA")
                .orElseThrow(() -> new IllegalStateException(
                        "Etapa final ENCERRADA não cadastrada"
                ));

        List<TipoDeEtapa> intermediarias;

        if (etapasId == null || etapasId.isEmpty()) {
            intermediarias = tipoDeEtapaRepository.findAllSystemEtapas()
                    .stream()
                    .filter(etapa -> !"INSCRICAO".equals(etapa.getCodigo()))
                    .filter(etapa -> !"ENCERRADA".equals(etapa.getCodigo()))
                    .sorted(java.util.Comparator.comparingInt(
                            etapa -> ordemEtapaSistema(etapa.getCodigo())
                    ))
                    .toList();
        } else {
            if (new HashSet<>(etapasId).size() != etapasId.size()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Não é permitido informar etapas duplicadas"
                );
            }

            intermediarias = tipoDeEtapaRepository.findAllById(etapasId);

            if (intermediarias.size() != new HashSet<>(etapasId).size()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Uma ou mais etapas informadas não existem"
                );
            }

            var etapasPorId = intermediarias.stream()
                    .collect(Collectors.toMap(
                            TipoDeEtapa::getId,
                            Function.identity()
                    ));

            intermediarias = etapasId.stream()
                    .map(etapasPorId::get)
                    .filter(etapa -> !"INSCRICAO".equals(etapa.getCodigo()))
                    .filter(etapa -> !"ENCERRADA".equals(etapa.getCodigo()))
                    .toList();
        }

        var etapasOrdenadas = new java.util.ArrayList<TipoDeEtapa>();
        etapasOrdenadas.add(etapaInicial);
        etapasOrdenadas.addAll(intermediarias);
        etapasOrdenadas.add(etapaFinal);

        for (var etapa : intermediarias) {
            if (!etapa.isSystem() && !etapa.getCompanyProfile().getId().equals(vaga.getEmpresa().getId())) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "Uma ou mais etapas informadas não existem");
            }
            if (!etapa.isDisponibilidade()) {
                throw new BusinessException(HttpStatus.CONFLICT, "A etapa '" + etapa.getNome() + "' está desabilitada");
            }
            etapa.marcarComoUtilizada();
        }

        vaga.setEtapas(
                java.util.stream.IntStream.range(0, etapasOrdenadas.size())
                        .mapToObj(i -> new VagaEtapa(
                                vaga,
                                etapasOrdenadas.get(i),
                                i + 1
                        ))
                        .toList()
        );
    }

    private int ordemEtapaSistema(String codigo) {
        return switch (codigo) {
            case "TRIAGEM" -> 1;
            case "ENTREVISTA_RH" -> 2;
            case "TESTE_TECNICO" -> 3;
            case "ENTREVISTA_TECNICA" -> 4;
            case "PROPOSTA" -> 5;
            case "INSCRICAO", "ENCERRADA" ->
                    throw new IllegalArgumentException(
                            "Etapa obrigatória não deve ser ordenada como intermediária: " + codigo
                    );
            default -> throw new IllegalStateException(
                    "Ordem padrão não definida para a etapa: " + codigo
            );
        };
    }
}