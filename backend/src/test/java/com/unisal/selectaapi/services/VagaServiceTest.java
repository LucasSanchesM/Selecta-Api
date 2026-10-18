package com.unisal.selectaapi.services;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.company.repository.CompanyProfileRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.geo.EnderecoDto;
import com.unisal.selectaapi.geo.GeoCodingService;
import com.unisal.selectaapi.vagas.dto.UpdateVaga;
import com.unisal.selectaapi.vagas.repository.VagaRepository;
import com.unisal.selectaapi.vagas.VagaRole;
import com.unisal.selectaapi.vagas.dto.CreateVagaDto;
import com.unisal.selectaapi.vagas.entity.ModalidadeVaga;
import com.unisal.selectaapi.vagas.entity.Vaga;
import com.unisal.selectaapi.vagas.services.VagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VagaServiceTest {

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private CompanyProfileRepository companyProfileRepository;

    @Mock
    private GeoCodingService geoCodingService;

    @InjectMocks
    private VagaService vagaService;

    @Captor
    private ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Captor
    private ArgumentCaptor<Double> doubleArgumentCaptor;

    @Captor
    private ArgumentCaptor<Vaga> vagaArgumentCaptor;

    @Nested
    class CreateVaga {

        //Pra facilitar vou utilizar esses registros de empresa e dto quando necessário nos testes
        private CompanyProfile empresa;
        private CreateVagaDto dto;
        @BeforeEach
        void setUp(){
            var id = UUID.randomUUID();
            empresa = new CompanyProfile();
            empresa.setId(id);
            empresa.setCompanyName("Empresa Teste LTDA");
            empresa.setCnpj("12345678000199");
            empresa.setDescription("Descrição da empresa teste");
            empresa.setIndustry("Tecnologia");
            empresa.setWebsite("https://empresateste.com");
            empresa.setCity("São Paulo");
            empresa.setState("SP");
            empresa.setLogoUrl("https://empresateste.com/logo.png");
            empresa.setLatitude(-23.5505);
            empresa.setLongitude(-46.6333);

            dto = new CreateVagaDto(
                    "Desenvolvedor Backend Java",
                    new BigDecimal("5000.00"),
                    "Vaga para desenvolvedor backend com experiência em Spring Boot",
                    "Vale alimentação, plano de saúde, home office",
                    id,
                    VagaRole.DESENVOLVIMENTO_SOFTWARE,
                    ModalidadeVaga.PRESENCIAL,
                    -23.5505,
                    -46.6333,
                    List.of(EtapaProcesso.ENTREVISTA_TECNICA, EtapaProcesso.ENTREVISTA_RH)
            );
        }

        @Test
        @DisplayName("Should create a vaga successfully")
        void shouldCreateAVagaSuccessfully(){
            //===== ARRANGE =======
            var input = dto;
            doReturn(Optional.of(empresa)).when(companyProfileRepository).findById(input.companyProfileId());
            var endereco = new EnderecoDto("Lorena", "SP");
            doReturn(endereco).when(geoCodingService).retornarEndereco(input.latitude(), input.longitude());
            when(vagaRepository.save(any(Vaga.class))).thenAnswer(chamado ->{
               return chamado.getArgument(0);
            });

            //===== ACT =======
            var response = vagaService.createVaga(input);

            //===== ASSERT =======
            //Verifica se busca por empresa foi feita e captura id usado pra chamado
            verify(companyProfileRepository, times(1)).findById(uuidArgumentCaptor.capture());
            //Assegura que id usado para busca de empresa é o mesmo passado nos parametros
            assertEquals(input.companyProfileId(), uuidArgumentCaptor.getValue());

            //Assegura que busca por endereco foi chamado
            verify(geoCodingService, times(1)).retornarEndereco(doubleArgumentCaptor.capture(), doubleArgumentCaptor.capture());
            var listOfDoubles = doubleArgumentCaptor.getAllValues();//index 0 -> latitude && index 1 -> longitude
            //Verifica se valores utilizados para busca de endeço bate com o inserido no metodo
            assertEquals(input.latitude(), listOfDoubles.get(0));
            assertEquals(input.longitude(), listOfDoubles.get(1));

            //Verifica que informaçoes foram salvas
            verify(vagaRepository, times(1)).save(vagaArgumentCaptor.capture());
            var vagaCaptured = vagaArgumentCaptor.getValue();
            //Assegura que o foi salvo no banco bate com o inserido no metodo
            assertEquals(input.titulo(), vagaCaptured.getTitulo());
            assertEquals(input.salario(), vagaCaptured.getSalario());
            assertEquals(input.descricao(), vagaCaptured.getDescricao());
            assertEquals(input.beneficios(), vagaCaptured.getBeneficios());
            assertEquals(input.companyProfileId(), vagaCaptured.getEmpresa().getId());
            assertEquals(input.categoria(), vagaCaptured.getCategoria());
            assertEquals(input.modalidade(), vagaCaptured.getModalidade());
            assertEquals(input.latitude(), vagaCaptured.getLatitude());
            assertEquals(input.longitude(), vagaCaptured.getLongitude());
            assertEquals(input.etapas().size(), vagaCaptured.getEtapas().size());

            //Pega lista de etapas salva no banco
            var etapas = vagaCaptured.getEtapas().stream().map(etapa -> etapa.getEtapa()).toList();

            assertEquals(input.etapas(), etapas);

            //Verifica se cidade e estado salvo sao os corretos
            assertEquals("Lorena", vagaCaptured.getCidade());
            assertEquals("SP", vagaCaptured.getEstado());


            //Assegura que o foi retornado bate com o inserido no metodo
            assertEquals(input.titulo(), response.titulo());
            assertEquals(input.salario(), response.salario());
            assertEquals(input.descricao(), response.descricao());
            assertEquals(input.beneficios(), response.beneficios());
            assertEquals(input.companyProfileId(), response.companyProfileId());
            assertEquals(input.categoria(), response.categoria());
            assertEquals(input.modalidade(), response.modalidade());
            assertEquals(input.latitude(), response.latitude());
            assertEquals(input.longitude(), response.longitude());

        }

        @Test
        @DisplayName("Should not create a vaga when company don't exist")
        void shouldNotCreateAVagaWhenCompanyDontExist(){
            //===== ARRANGE =======
            var input = dto;
            doReturn(Optional.empty()).when(companyProfileRepository).findById(input.companyProfileId());

            //===== ACT =======
            var response = assertThrows(BusinessException.class, ()-> vagaService.createVaga(input));

            //===== ASSERT =======
            //Garante que nenhuma info foi salva no banco de dados
            verify(vagaRepository, never()).save(any(Vaga.class));

            //Assegura que busca por empresas foi feito
            verify(companyProfileRepository, times(1)).findById(uuidArgumentCaptor.capture());
            //Verifica que id usado busca por empresa é o mesmo presente no dto de input
            assertEquals(input.companyProfileId(), uuidArgumentCaptor.getValue());
            assertEquals("Empresa não encontrada com o id: " + uuidArgumentCaptor.getValue(), response.getMessage());

        }

        @Test
        @DisplayName("Should not create a vaga when etapa contain incrição or contratação")
        void shouldNotCreateAVagaWhenEtapaContainIncricaoOrContratacao(){
            //===== ARRANGE =======
            var input = new CreateVagaDto(
                    "Desenvolvedor Backend Java",
                    new BigDecimal("5000.00"),
                    "Vaga para desenvolvedor backend com experiência em Spring Boot",
                    "Vale alimentação, plano de saúde, home office",
                    dto.companyProfileId(),
                    VagaRole.DESENVOLVIMENTO_SOFTWARE,
                    ModalidadeVaga.PRESENCIAL,
                    -23.5505,
                    -46.6333,
                     List.of(EtapaProcesso.INSCRICAO, EtapaProcesso.ENTREVISTA_RH, EtapaProcesso.CONTRATACAO)
            );
            doReturn(Optional.of(empresa)).when(companyProfileRepository).findById(input.companyProfileId());
            var endereco = new EnderecoDto("Lorena", "SP");
            doReturn(endereco).when(geoCodingService).retornarEndereco(input.latitude(), input.longitude());

            //===== ACT =======
            var response = assertThrows(BusinessException.class, ()-> vagaService.createVaga(input));

            //===== ASSERT =======
            verify(companyProfileRepository, times(1)).findById(uuidArgumentCaptor.capture());
            assertEquals(input.companyProfileId(), uuidArgumentCaptor.getValue());

            verify(geoCodingService, times(1)).retornarEndereco(doubleArgumentCaptor.capture(), doubleArgumentCaptor.capture());
            var listOfDoubles = doubleArgumentCaptor.getAllValues();

            assertEquals(dto.latitude(), listOfDoubles.get(0));
            assertEquals(dto.longitude(), listOfDoubles.get(1));

            verify(vagaRepository, never()).save(any(Vaga.class));

            assertEquals("A vaga deve possuir etapas configuráveis distintas, sem INSCRICAO ou CONTRATACAO", response.getMessage());


        }

        @Test
        @DisplayName("Should create a vaga successfully when etapas is blank")
        void shouldCreateAVagaSuccessfullyWhenEtapasIsBlank(){
            var input = new CreateVagaDto(
                    "Desenvolvedor Backend Java",
                    new BigDecimal("5000.00"),
                    "Vaga para desenvolvedor backend com experiência em Spring Boot",
                    "Vale alimentação, plano de saúde, home office",
                    dto.companyProfileId(),
                    VagaRole.DESENVOLVIMENTO_SOFTWARE,
                    ModalidadeVaga.PRESENCIAL,
                    -23.5505,
                    -46.6333,
                    Collections.emptyList()
            );
            doReturn(Optional.of(empresa)).when(companyProfileRepository).findById(input.companyProfileId());
            var endereco = new EnderecoDto("Lorena", "SP");
            doReturn(endereco).when(geoCodingService).retornarEndereco(input.latitude(), input.longitude());
            when(vagaRepository.save(any(Vaga.class))).thenAnswer(chamado ->{
                return chamado.getArgument(0);
            });

            //===== ACT =======
            var response = vagaService.createVaga(input);

            //===== ASSERT =======
            //Verifica se busca por empresa foi feita e captura id usado pra chamado
            verify(companyProfileRepository, times(1)).findById(uuidArgumentCaptor.capture());
            //Assegura que id usado para busca de empresa é o mesmo passado nos parametros
            assertEquals(input.companyProfileId(), uuidArgumentCaptor.getValue());

            //Assegura que busca por endereco foi chamado
            verify(geoCodingService, times(1)).retornarEndereco(doubleArgumentCaptor.capture(), doubleArgumentCaptor.capture());
            var listOfDoubles = doubleArgumentCaptor.getAllValues();//index 0 -> latitude && index 1 -> longitude
            //Verifica se valores utilizados para busca de endeço bate com o inserido no metodo
            assertEquals(input.latitude(), listOfDoubles.get(0));
            assertEquals(input.longitude(), listOfDoubles.get(1));

            //Verifica que informaçoes foram salvas
            verify(vagaRepository, times(1)).save(vagaArgumentCaptor.capture());
            var vagaCaptured = vagaArgumentCaptor.getValue();
            //Assegura que o foi salvo no banco bate com o inserido no metodo
            assertEquals(input.titulo(), vagaCaptured.getTitulo());
            assertEquals(input.salario(), vagaCaptured.getSalario());
            assertEquals(input.descricao(), vagaCaptured.getDescricao());
            assertEquals(input.beneficios(), vagaCaptured.getBeneficios());
            assertEquals(input.companyProfileId(), vagaCaptured.getEmpresa().getId());
            assertEquals(input.categoria(), vagaCaptured.getCategoria());
            assertEquals(input.modalidade(), vagaCaptured.getModalidade());
            assertEquals(input.latitude(), vagaCaptured.getLatitude());
            assertEquals(input.longitude(), vagaCaptured.getLongitude());

            //Pega lista de etapas salva no banco
            var etapas = vagaCaptured.getEtapas().stream().map(etapa -> etapa.getEtapa()).toList();

            var etapasPadrao = List.of( EtapaProcesso.TRIAGEM,
                                        EtapaProcesso.ENTREVISTA_RH,
                                        EtapaProcesso.TESTE_TECNICO,
                                        EtapaProcesso.ENTREVISTA_TECNICA,
                                        EtapaProcesso.PROPOSTA);
            assertEquals(etapasPadrao, etapas);

            //Verifica se cidade e estado salvo sao os corretos
            assertEquals("Lorena", vagaCaptured.getCidade());
            assertEquals("SP", vagaCaptured.getEstado());


            //Assegura que o foi retornado bate com o inserido no metodo
            assertEquals(input.titulo(), response.titulo());
            assertEquals(input.salario(), response.salario());
            assertEquals(input.descricao(), response.descricao());
            assertEquals(input.beneficios(), response.beneficios());
            assertEquals(input.companyProfileId(), response.companyProfileId());
            assertEquals(input.categoria(), response.categoria());
            assertEquals(input.modalidade(), response.modalidade());
            assertEquals(input.latitude(), response.latitude());
            assertEquals(input.longitude(), response.longitude());

        }

    }

    @Nested
    class FindAllWithFilters{
        private CompanyProfile empresa;
        private Vaga vaga;

        @BeforeEach
        void setUp(){
            empresa = new CompanyProfile();
            empresa.setId(UUID.randomUUID());
            empresa.setCompanyName("Empresa X");
            empresa.setCnpj("12345678000199");
            empresa.setLogoUrl("https://exemplo.com/logo.png");

            vaga = new Vaga();
            vaga.setId(UUID.randomUUID());
            vaga.setTitulo("Desenvolvedor Java");
            vaga.setSalario(new BigDecimal("8000.00"));
            vaga.setDescricao("Desenvolvimento de APIs REST com Spring Boot");
            vaga.setBeneficios("VR, VA, plano de saúde");
            vaga.setEmpresa(empresa);
            vaga.setCidade("São Paulo");
            vaga.setEstado("SP");
            vaga.setLatitude(-23.5505);
            vaga.setLongitude(-46.6333);
            vaga.setCategoria(VagaRole.DESENVOLVIMENTO_SOFTWARE);
            vaga.setModalidade(ModalidadeVaga.PRESENCIAL);
            vaga.setAtiva(true);
            vaga.setCreatedAt(Instant.now());
            vaga.setUpdatedAt(Instant.now());
        }

        //Aqui, como o metodo em si nao lança nenhuma excessão irei testar somente o funcionamento das chamadas de find dtc
        @Test
        @DisplayName("Should return a vaga with success")
        void shouldReturnAVagaWithSuccess(){
            //===== ARRANGE =======
            var inputTitle = "Desenvolvedor Java";
            when(vagaRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(vaga)));
            //===== ACT =======
            var response = vagaService.findAllWithFilters(inputTitle,
                                                    null, null,
                                                    null, null,
                                                        null, null,
                                                        null, null,
                                                        0, 1);
            //===== ASSERT =======
            verify(vagaRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
            assertEquals(vaga.getId(), response.get(0).id());
        }
    }

    @Nested
    class FindAllByCompany{

        private UUID id;
        private CompanyProfile empresa;
        private Vaga vaga1;
        private Vaga vaga2;

        @BeforeEach
        void setUp(){
            id = UUID.randomUUID();
            empresa = new CompanyProfile();
            empresa.setId(id);
            empresa.setCompanyName("Empresa Teste LTDA");
            empresa.setCnpj("12345678000199");
            empresa.setDescription("Descrição da empresa teste");
            empresa.setIndustry("Tecnologia");
            empresa.setWebsite("https://empresateste.com");
            empresa.setCity("São Paulo");
            empresa.setState("SP");
            empresa.setLogoUrl("https://empresateste.com/logo.png");
            empresa.setLatitude(-23.5505);
            empresa.setLongitude(-46.6333);

            vaga1 = new Vaga();
            vaga1.setId(UUID.randomUUID());
            vaga1.setTitulo("Desenvolvedor Java");
            vaga1.setSalario(new BigDecimal("8000.00"));
            vaga1.setDescricao("Desenvolvimento de APIs REST com Spring Boot");
            vaga1.setBeneficios("VR, VA, plano de saúde");
            vaga1.setEmpresa(empresa);
            vaga1.setCidade("São Paulo");
            vaga1.setEstado("SP");
            vaga1.setLatitude(-23.5505);
            vaga1.setLongitude(-46.6333);
            vaga1.setCategoria(VagaRole.DESENVOLVIMENTO_SOFTWARE);
            vaga1.setModalidade(ModalidadeVaga.PRESENCIAL);
            vaga1.setAtiva(true);
            vaga1.setCreatedAt(Instant.now());
            vaga1.setUpdatedAt(Instant.now());

            vaga2 = new Vaga();
            vaga2.setId(UUID.randomUUID());
            vaga2.setTitulo("Analista de Dados Pleno");
            vaga2.setSalario(new BigDecimal("9500.00"));
            vaga2.setDescricao("Vaga para analista de dados com experiência em SQL, Python e Power BI");
            vaga2.setBeneficios("Vale refeição, Gympass, participação nos lucros, auxílio home office");
            vaga2.setEmpresa(empresa);
            vaga2.setCidade("Rio de Janeiro");
            vaga2.setEstado("RJ");
            vaga2.setLatitude(-22.9068);
            vaga2.setLongitude(-43.1729);
            vaga2.setCategoria(VagaRole.INFRAESTRUTURA_DEVOPS);
            vaga2.setModalidade(ModalidadeVaga.REMOTO);
            vaga2.setAtiva(true);
            vaga2.setCreatedAt(Instant.now());
            vaga2.setUpdatedAt(Instant.now());
        }

        @Test
        @DisplayName("Should return all vaga's company with success")
        void shouldReturnAllVagasCompanyWithSucess(){
            //===== ARRANGE =======
            var input = id;

            doReturn(Optional.of(empresa)).when(companyProfileRepository).findById(input);
            doReturn(List.of(vaga1, vaga2)).when(vagaRepository).findByEmpresaId(input);

            //===== ACT =======
            var response = vagaService.findAllByCompany(input);

            //===== ASSERT =======
            verify(companyProfileRepository, times(1)).findById(uuidArgumentCaptor.capture());
            verify(vagaRepository, times(1)).findByEmpresaId(uuidArgumentCaptor.capture());
            var listOfids = uuidArgumentCaptor.getAllValues();
            assertEquals(empresa.getId(), listOfids.get(0));
            assertEquals(vaga1.getEmpresa().getId(), listOfids.get(1));
            assertEquals(2, response.size());

            //----- Vaga 1 -----
            assertEquals(vaga1.getId(), response.get(0).id());
            assertEquals(vaga1.getTitulo(), response.get(0).titulo());
            assertEquals(vaga1.getSalario(), response.get(0).salario());
            assertEquals(vaga1.getDescricao(), response.get(0).descricao());
            assertEquals(vaga1.getBeneficios(), response.get(0).beneficios());
            assertEquals(vaga1.getCidade(), response.get(0).cidade());
            assertEquals(vaga1.getEstado(), response.get(0).estado());
            assertEquals(vaga1.getLatitude(), response.get(0).latitude());
            assertEquals(vaga1.getLongitude(), response.get(0).longitude());
            assertEquals(vaga1.getCategoria(), response.get(0).categoria());
            assertEquals(vaga1.getModalidade(), response.get(0).modalidade());
            assertEquals(vaga1.getAtiva(), response.get(0).ativa());

            //----- Vaga 2 -----
            assertEquals(vaga2.getId(), response.get(1).id());
            assertEquals(vaga2.getTitulo(), response.get(1).titulo());
            assertEquals(vaga2.getSalario(), response.get(1).salario());
            assertEquals(vaga2.getDescricao(), response.get(1).descricao());
            assertEquals(vaga2.getBeneficios(), response.get(1).beneficios());
            assertEquals(vaga2.getCidade(), response.get(1).cidade());
            assertEquals(vaga2.getEstado(), response.get(1).estado());
            assertEquals(vaga2.getLatitude(), response.get(1).latitude());
            assertEquals(vaga2.getLongitude(), response.get(1).longitude());
            assertEquals(vaga2.getCategoria(), response.get(1).categoria());
            assertEquals(vaga2.getModalidade(), response.get(1).modalidade());
            assertEquals(vaga2.getAtiva(), response.get(1).ativa());

            //Verica se é a mesma empresa nas vagas
            assertEquals(empresa.getId(), response.get(0).companyProfileId());
            assertEquals(empresa.getId(), response.get(1).companyProfileId());
        }

        @Test
        @DisplayName("Should not return any vaga when company don't exist")
        void shouldNotReturnAnyVagaWhenCompanyDontExist(){
            //===== ARRANGE =======
            var input = id;

            doReturn(Optional.empty()).when(companyProfileRepository).findById(input);

            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> vagaService.findAllByCompany(input));

            //===== ASSERT =======
            verify(companyProfileRepository, times(1)).findById(uuidArgumentCaptor.capture());
            verify(vagaRepository, never()).findByEmpresaId(any(UUID.class));
            assertEquals(input, uuidArgumentCaptor.getValue());
            assertEquals("Empresa não encontrada com o id: " + input, response.getMessage());
        }

    }

    @Nested
    class  FindVagaById{
        private CompanyProfile empresa;
        private Vaga vaga;
        private UUID vagaId;

        @BeforeEach
        void setUp(){
            empresa = new CompanyProfile();
            empresa.setId(UUID.randomUUID());
            empresa.setCompanyName("Empresa X");
            empresa.setCnpj("12345678000199");
            empresa.setLogoUrl("https://exemplo.com/logo.png");

            vagaId = UUID.randomUUID();
            vaga = new Vaga();
            vaga.setId(vagaId);
            vaga.setTitulo("Desenvolvedor Java");
            vaga.setSalario(new BigDecimal("8000.00"));
            vaga.setDescricao("Desenvolvimento de APIs REST com Spring Boot");
            vaga.setBeneficios("VR, VA, plano de saúde");
            vaga.setEmpresa(empresa);
            vaga.setCidade("São Paulo");
            vaga.setEstado("SP");
            vaga.setLatitude(-23.5505);
            vaga.setLongitude(-46.6333);
            vaga.setCategoria(VagaRole.DESENVOLVIMENTO_SOFTWARE);
            vaga.setModalidade(ModalidadeVaga.PRESENCIAL);
            vaga.setAtiva(true);
            vaga.setCreatedAt(Instant.now());
            vaga.setUpdatedAt(Instant.now());
        }

        @Test
        @DisplayName("Should return a vaga with success")
        void shouldReturnVagaWithSuccess(){
            //===== ARRANGE =======
            var input = vagaId;
            doReturn(Optional.of(vaga)).when(vagaRepository).findById(input);

            //===== ACT =======
            var response = vagaService.findVagaById(input);

            //===== ASSERT =======
            verify(vagaRepository, times(1)).findById(uuidArgumentCaptor.capture());
            assertEquals(input, uuidArgumentCaptor.getValue());

            assertEquals(response.id(), input);
        }

        @Test
        @DisplayName("Should not return vaga when it dont exist")
        void shouldNotReturnVagaWhenItDontExist(){
            //===== ARRANGE =======
            var input = vagaId;
            doReturn(Optional.empty()).when(vagaRepository).findById(input);

            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> vagaService.findVagaById(input));

            //===== ASSERT =======
            verify(vagaRepository, times(1)).findById(uuidArgumentCaptor.capture());
            assertEquals(input, uuidArgumentCaptor.getValue());

            assertEquals("Vaga não encontrada", response.getMessage());

        }

    }

    @Nested
    class UpdateVagaTest{
        private CompanyProfile empresa;
        private Vaga vaga;
        private UUID vagaId;
        private UpdateVaga dto;

        @BeforeEach
        void setUp(){
            empresa = new CompanyProfile();
            empresa.setId(UUID.randomUUID());
            empresa.setCompanyName("Empresa X");
            empresa.setCnpj("12345678000199");
            empresa.setLogoUrl("https://exemplo.com/logo.png");

            vagaId = UUID.randomUUID();
            vaga = new Vaga();
            vaga.setId(vagaId);
            vaga.setTitulo("Desenvolvedor Java");
            vaga.setSalario(new BigDecimal("8000.00"));
            vaga.setDescricao("Desenvolvimento de APIs REST com Spring Boot");
            vaga.setBeneficios("VR, VA, plano de saúde");
            vaga.setEmpresa(empresa);
            vaga.setCidade("São Paulo");
            vaga.setEstado("SP");
            vaga.setLatitude(-23.5505);
            vaga.setLongitude(-46.6333);
            vaga.setCategoria(VagaRole.DESENVOLVIMENTO_SOFTWARE);
            vaga.setModalidade(ModalidadeVaga.PRESENCIAL);
            vaga.setAtiva(true);
            vaga.setCreatedAt(Instant.now());
            vaga.setUpdatedAt(Instant.now());

            dto = new UpdateVaga(
                    "Desenvolvedor Backend Java Sênior",
                    new BigDecimal("12000.00"),
                    "Vaga atualizada para desenvolvedor backend sênior com Spring Boot e microsserviços",
                    "VR, VA, plano de saúde, plano odontológico, home office",
                    -22.7306,
                    -45.1243,
                    VagaRole.DESENVOLVIMENTO_SOFTWARE,
                    ModalidadeVaga.REMOTO,
                    false);
        }

        @Test
        @DisplayName("Should update a vaga with success")
        void shouldUpdateAVagaWithSuccess(){
            //===== ARRANGE =======
            var inputId = vagaId;
            var inputDto = dto;
            doReturn(Optional.of(vaga)).when(vagaRepository).findById(inputId);
            var endereco = new EnderecoDto("Lorena", "SP");
            doReturn(endereco).when(geoCodingService).retornarEndereco(inputDto.latitude(), inputDto.longitude());
            when(vagaRepository.save(any(Vaga.class))).thenAnswer(chamado ->{
                return chamado.getArgument(0);
            });

            //===== ACT =======
            var response = vagaService.updateVaga(inputId, inputDto);

            //===== ASSERT =======
            verify(vagaRepository, times(1)).findById(uuidArgumentCaptor.capture());
            assertEquals(inputId, uuidArgumentCaptor.getValue());

            verify(geoCodingService, times(1)).retornarEndereco(doubleArgumentCaptor.capture(), doubleArgumentCaptor.capture());
            var listOfDoubles = doubleArgumentCaptor.getAllValues();//index 0 -> latitude && index 1 -> longitude
            assertEquals(inputDto.latitude(), listOfDoubles.get(0));
            assertEquals(inputDto.longitude(), listOfDoubles.get(1));

            verify(vagaRepository, times(1)).save(vagaArgumentCaptor.capture());
            var vagaCaptured = vagaArgumentCaptor.getValue();

            //Verifica se vaga que seria salva bate com o conteudo do dto
            assertEquals(inputId, vagaCaptured.getId());
            assertEquals(inputDto.titulo(), vagaCaptured.getTitulo());
            assertEquals(inputDto.salario(), vagaCaptured.getSalario());
            assertEquals(inputDto.descricao(), vagaCaptured.getDescricao());
            assertEquals(inputDto.beneficios(), vagaCaptured.getBeneficios());
            assertEquals(inputDto.latitude(), vagaCaptured.getLatitude());
            assertEquals(inputDto.longitude(), vagaCaptured.getLongitude());
            assertEquals(inputDto.categoria(), vagaCaptured.getCategoria());
            assertEquals(inputDto.modalidade(), vagaCaptured.getModalidade());
            assertEquals(inputDto.ativa(), vagaCaptured.getAtiva());

            //Verifica que vaga capturada teve campos cidade e estado atualizados corretamente
            assertEquals("Lorena", vagaCaptured.getCidade());
            assertEquals("SP", vagaCaptured.getEstado());

            //Assegura que as infos inseridas no dto foram atualizados e retornados corretamente
            assertEquals(vagaId, response.id());
            assertEquals(inputDto.titulo(), response.titulo());
            assertEquals(inputDto.salario(), response.salario());
            assertEquals(inputDto.descricao(), response.descricao());
            assertEquals(inputDto.beneficios(), response.beneficios());
            assertEquals(inputDto.latitude(), response.latitude());
            assertEquals(inputDto.longitude(), response.longitude());
            assertEquals(inputDto.categoria(), response.categoria());
            assertEquals(inputDto.modalidade(), response.modalidade());
            assertEquals(inputDto.ativa(), response.ativa());
            assertEquals("Lorena", response.cidade());
            assertEquals("SP", response.estado());
            assertEquals(empresa.getId(), response.companyProfileId());
        }

        @Test
        @DisplayName("Should not update a vaga when vagaId dont exist")
        void shouldNotUpdateAVagaWhenVagaIdDontExist(){
            //===== ARRANGE =======
            var inputId = vagaId;
            var inputDto = dto;
            doReturn(Optional.empty()).when(vagaRepository).findById(inputId);

            //===== ACT =======
            var response = assertThrows(BusinessException.class, ()->vagaService.updateVaga(inputId, inputDto));

            //===== ASSERT =======
            verify(vagaRepository, times(1)).findById(uuidArgumentCaptor.capture());
            assertEquals(inputId, uuidArgumentCaptor.getValue());

            //Verifica que nenhuma chamada a mais foi feito apos o erro
            verify(geoCodingService, never()).retornarEndereco(anyDouble(), anyDouble());
            verify(vagaRepository, never()).save(any(Vaga.class));

            assertEquals("Vaga não encontrada", response.getMessage());
        }
    }

    @Nested
    class DeleteVaga{
        private CompanyProfile empresa;
        private Vaga vaga;
        private UUID vagaId;
        @BeforeEach
        void setUp(){
            empresa = new CompanyProfile();
            empresa.setId(UUID.randomUUID());
            empresa.setCompanyName("Empresa X");
            empresa.setCnpj("12345678000199");
            empresa.setLogoUrl("https://exemplo.com/logo.png");

            vagaId = UUID.randomUUID();
            vaga = new Vaga();
            vaga.setId(vagaId);
            vaga.setTitulo("Desenvolvedor Java");
            vaga.setSalario(new BigDecimal("8000.00"));
            vaga.setDescricao("Desenvolvimento de APIs REST com Spring Boot");
            vaga.setBeneficios("VR, VA, plano de saúde");
            vaga.setEmpresa(empresa);
            vaga.setCidade("São Paulo");
            vaga.setEstado("SP");
            vaga.setLatitude(-23.5505);
            vaga.setLongitude(-46.6333);
            vaga.setCategoria(VagaRole.DESENVOLVIMENTO_SOFTWARE);
            vaga.setModalidade(ModalidadeVaga.PRESENCIAL);
            vaga.setAtiva(true);
            vaga.setCreatedAt(Instant.now());
            vaga.setUpdatedAt(Instant.now());
        }

        @Test
        @DisplayName("Should delete a vaga with success")
        void shouldDeleteAVagaWithSuccess(){
            //===== ARRANGE =======
            var input = vagaId;
            doReturn(Optional.of(vaga)).when(vagaRepository).findById(input);

            //===== ACT =======
            vagaService.deleteVaga(input);

            //===== ASSERT =======
            verify(vagaRepository, times(1)).findById(uuidArgumentCaptor.capture());

            verify(vagaRepository, times(1)).delete(vagaArgumentCaptor.capture());
            var vagaCaptured= vagaArgumentCaptor.getValue();
            assertEquals(input, uuidArgumentCaptor.getValue());
            assertEquals(input, vagaCaptured.getId());

        }

        @Test
        @DisplayName("Should not delete a vaga when vagaId dont exist")
        void shouldNotDeleteAVagaWhenVagaIdDontExist(){
            //===== ARRANGE =======
            var input = vagaId;
            doReturn(Optional.empty()).when(vagaRepository).findById(input);

            //===== ACT =======
            var response = assertThrows(BusinessException.class, ()->vagaService.deleteVaga(input));

            //===== ASSERT =======
            verify(vagaRepository, times(1)).findById(uuidArgumentCaptor.capture());
            verify(vagaRepository, never()).delete(any(Vaga.class));
            assertEquals("Vaga não encontrada", response.getMessage());

        }
    }
}

