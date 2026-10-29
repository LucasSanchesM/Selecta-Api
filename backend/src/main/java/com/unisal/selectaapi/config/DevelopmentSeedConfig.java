package com.unisal.selectaapi.config;

import java.math.BigDecimal;
import java.util.List;

import com.unisal.selectaapi.vagas.entity.TipoDeEtapa;
import com.unisal.selectaapi.vagas.repository.TipoDeEtapaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.company.repository.CompanyProfileRepository;
import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.candidate.repository.CandidateProfileRepository;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserRole;
import com.unisal.selectaapi.user.entity.User;
import com.unisal.selectaapi.vagas.repository.VagaRepository;
import com.unisal.selectaapi.vagas.VagaRole;
import com.unisal.selectaapi.vagas.entity.ModalidadeVaga;
import com.unisal.selectaapi.vagas.entity.Vaga;
import com.unisal.selectaapi.vagas.entity.VagaEtapa;

@Configuration
public class DevelopmentSeedConfig {

    private TipoDeEtapa systemStage(
            TipoDeEtapaRepository repository,
            String codigo,
            String nome) {

        return repository.findByCodigoAndCompanyProfileIsNull(codigo)
                .orElseGet(() -> {
                    TipoDeEtapa etapa = new TipoDeEtapa(null, nome);
                    etapa.setCodigo(codigo);
                    return repository.save(etapa);
                });
    }

    @Bean
    CommandLineRunner developmentSeed(
            @Value("${app.seed.enabled:false}") boolean seedEnabled,
            UserRepository userRepository,
            CompanyProfileRepository companyProfileRepository,
            CandidateProfileRepository candidateProfileRepository,
            VagaRepository vagaRepository,
            TipoDeEtapaRepository tipoDeEtapaRepository,
            BCryptPasswordEncoder passwordEncoder) {

        return args -> {
            if (!seedEnabled) {
                return;
            }

            TipoDeEtapa inscricao = systemStage(tipoDeEtapaRepository, "INSCRICAO", "Inscrição");
            TipoDeEtapa triagem = systemStage(tipoDeEtapaRepository, "TRIAGEM", "Triagem");
            TipoDeEtapa entrevistaRh = systemStage(tipoDeEtapaRepository, "ENTREVISTA_RH", "Entrevista com RH");
            TipoDeEtapa testeTecnico = systemStage(tipoDeEtapaRepository, "TESTE_TECNICO", "Teste técnico");
            TipoDeEtapa entrevistaTecnica = systemStage(tipoDeEtapaRepository, "ENTREVISTA_TECNICA", "Entrevista técnica");
            TipoDeEtapa proposta = systemStage(tipoDeEtapaRepository, "PROPOSTA", "Proposta");
            TipoDeEtapa encerrada = systemStage(tipoDeEtapaRepository, "ENCERRADA", "Encerrada");

            List<TipoDeEtapa> etapasPadrao = List.of(
                    inscricao,
                    triagem,
                    entrevistaRh,
                    testeTecnico,
                    entrevistaTecnica,
                    proposta,
                    encerrada
            );

            CompanyProfile techNova = company(
                    userRepository, companyProfileRepository, passwordEncoder,
                    "TechNova Solucoes", "rh@technova.dev", "29999999000101",
                    "Tecnologia", "Sao Paulo", "SP",
                    "Empresa de produtos digitais e plataformas web.",
                    -23.5505, -46.6333);

            CompanyProfile ponteVerde = company(
                    userRepository, companyProfileRepository, passwordEncoder,
                    "Ponte Verde Consultoria", "talentos@ponteverde.com.br", "28888888000102",
                    "Consultoria empresarial", "Campinas", "SP",
                    "Consultoria focada em pessoas, processos e crescimento sustentavel.",
                    -22.9056, -47.0608);

            CompanyProfile horizonteSaude = company(userRepository, companyProfileRepository, passwordEncoder,
                    "Horizonte Saude", "pessoas@horizontesaude.com.br", "27777777000103",
                    "Saude", "Sao Jose dos Campos", "SP", "Rede de cuidado e tecnologia em saude.",
                    -23.1896, -45.8841);
            CompanyProfile rotaCerta = company(userRepository, companyProfileRepository, passwordEncoder,
                    "Rota Certa Logistica", "talentos@rotacerta.com.br", "26666666000104",
                    "Logistica", "Guarulhos", "SP", "Operacao logistica integrada para todo o Brasil.",
                    -23.4543, -46.5337);
            CompanyProfile casaClara = company(userRepository, companyProfileRepository, passwordEncoder,
                    "Casa Clara", "gente@casaclara.com.br", "25555555000105",
                    "Varejo", "Santos", "SP", "Marca de casa, decoracao e bem-estar.",
                    -23.9608, -46.3336);
            CompanyProfile aguaViva = company(userRepository, companyProfileRepository, passwordEncoder,
                    "Agua Viva Ambiental", "pessoas@aguaviva.eco", "24444444000106",
                    "Meio ambiente", "Sorocaba", "SP", "Solucoes ambientais para cidades mais sustentaveis.",
                    -23.5015, -47.4526);
            CompanyProfile ponteSul = company(userRepository, companyProfileRepository, passwordEncoder,
                    "Ponte Sul Engenharia", "rh@pontesul.eng.br", "23333333000107",
                    "Engenharia", "Sao Paulo", "SP", "Projetos de infraestrutura e construcao civil.",
                    -23.5505, -46.6333);
            CompanyProfile farolEdu = company(userRepository, companyProfileRepository, passwordEncoder,
                    "Farol Educacao", "carreiras@faroledu.com.br", "22222222000108",
                    "Educacao", "Jundiai", "SP", "Tecnologia e aprendizagem para transformar carreiras.",
                    -23.1857, -46.8978);
            CompanyProfile saborDaTerra = company(userRepository, companyProfileRepository, passwordEncoder,
                    "Sabor da Terra", "gente@sabordaterra.com.br", "21111111000109",
                    "Alimentos", "Ribeirao Preto", "SP", "Produtos alimenticios com origem e qualidade.",
                    -21.1704, -47.8103);
            CompanyProfile novaMente = company(userRepository, companyProfileRepository, passwordEncoder,
                    "NovaMente Tecnologia", "pessoas@novamente.tech", "20000000000110",
                    "Tecnologia", "Sao Jose dos Campos", "SP", "Produtos digitais para aprendizagem e colaboracao.",
                    -23.1896, -45.8841);

            candidate(candidateProfileRepository, userRepository, passwordEncoder,
                    "Ana Souza", "ana.souza@candidatos.dev", "Desenvolvedora Java", "Sao Paulo", "SP", 1);
            candidate(candidateProfileRepository, userRepository, passwordEncoder,
                    "Bruno Lima", "bruno.lima@candidatos.dev", "Analista de projetos", "Campinas", "SP", 2);
            candidate(candidateProfileRepository, userRepository, passwordEncoder,
                    "Carla Mendes", "carla.mendes@candidatos.dev", "Desenvolvedora frontend", "Santos", "SP", 3);
            candidate(candidateProfileRepository, userRepository, passwordEncoder,
                    "Diego Oliveira", "diego.oliveira@candidatos.dev", "Analista de dados", "Jundiai", "SP", 4);

            createJobIfAbsent(vagaRepository, techNova, "Desenvolvedor(a) Backend Java",
                    new BigDecimal("8500.00"),
                    "Desenvolver APIs REST com Spring Boot, PostgreSQL e testes automatizados.",
                    "Vale refeicao, plano de saude, trabalho remoto e horario flexivel.",
                    "Sao Paulo", "SP", -23.5505, -46.6333,
                    VagaRole.DESENVOLVIMENTO_SOFTWARE, ModalidadeVaga.REMOTO,
                    List.of(inscricao, triagem, testeTecnico, entrevistaTecnica, proposta, encerrada));

            createJobIfAbsent(vagaRepository, techNova, "Pessoa Desenvolvedora Frontend",
                    new BigDecimal("6500.00"),
                    "Construir interfaces acessiveis com React, TypeScript e integracao com APIs.",
                    "Vale refeicao, auxilio home office e plano odontologico.",
                    "Sao Paulo", "SP", -23.5505, -46.6333,
                    VagaRole.DESENVOLVIMENTO_SOFTWARE, ModalidadeVaga.HIBRIDO,
                    List.of(inscricao, triagem, entrevistaRh, testeTecnico, proposta, encerrada));


            createJobIfAbsent(vagaRepository, ponteVerde, "Analista de Projetos",
                    new BigDecimal("5800.00"),
                    "Acompanhar cronogramas, riscos, indicadores e comunicacao com clientes.",
                    "Vale alimentacao, plano de saude e programa de desenvolvimento.",
                    "Campinas", "SP", -22.9056, -47.0608,
                    VagaRole.GESTAO_PROJETOS, ModalidadeVaga.HIBRIDO,
                    List.of(inscricao, triagem, entrevistaRh, testeTecnico, proposta, encerrada));


            createJobIfAbsent(vagaRepository, ponteVerde, "Estagio em Recursos Humanos",
                    new BigDecimal("1800.00"),
                    "Apoiar processos de recrutamento, entrevistas e organizacao de documentos.",
                    "Bolsa auxilio, vale transporte e mentoria.",
                    "Campinas", "SP", -22.9056, -47.0608,
                    VagaRole.RECURSOS_HUMANOS, ModalidadeVaga.PRESENCIAL,
                    List.of(inscricao, triagem, entrevistaRh, proposta, encerrada));

            createGeneratedJobs(
                    vagaRepository,
                    List.of(
                            techNova, ponteVerde, horizonteSaude, rotaCerta,
                            casaClara, aguaViva, ponteSul, farolEdu,
                            saborDaTerra, novaMente
                    ),
                    etapasPadrao
            );
        };
    }

    private CompanyProfile company(
            UserRepository userRepository,
            CompanyProfileRepository companyProfileRepository,
            BCryptPasswordEncoder passwordEncoder,
            String responsibleName,
            String email,
            String cnpj,
            String industry,
            String city,
            String state,
            String description,
            Double latitude,
            Double longitude) {

        User user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(new User(
                responsibleName,
                email,
                passwordEncoder.encode("Teste@123"),
                UserRole.COMPANY.name(),
                true)));
        user.setActive(true);
        userRepository.save(user);

                CompanyProfile existingProfile = companyProfileRepository.findByUserId(user.getId()).orElse(null);
                if (existingProfile != null) {
                        if (existingProfile.getLatitude() == null || existingProfile.getLongitude() == null) {
                                existingProfile.setLatitude(latitude);
                                existingProfile.setLongitude(longitude);
                        }
                        if (existingProfile.getLogoUrl() == null || existingProfile.getLogoUrl().isBlank()) {
                                existingProfile.setLogoUrl(companyImageUrl(responsibleName));
                        }
                        companyProfileRepository.save(existingProfile);
                        return existingProfile;
                }

                return companyProfileRepository.save(newCompanyProfile(user, responsibleName, cnpj, industry,
                                city, state, description, latitude, longitude));
        }

        private CompanyProfile newCompanyProfile(
                        User user,
                        String companyName,
                        String cnpj,
                        String industry,
                        String city,
                        String state,
                        String description,
                        Double latitude,
                        Double longitude) {

            CompanyProfile profile = new CompanyProfile(user, companyName);
            profile.setCnpj(cnpj);
            profile.setIndustry(industry);
            profile.setCity(city);
            profile.setState(state);
            profile.setDescription(description);
                        profile.setLogoUrl(companyImageUrl(companyName));
                        profile.setLatitude(latitude);
                        profile.setLongitude(longitude);
                        return profile;
    }

        private void candidate(
                        CandidateProfileRepository candidateProfileRepository,
                        UserRepository userRepository,
                        BCryptPasswordEncoder passwordEncoder,
                        String name,
                        String email,
                        String headline,
                        String city,
                        String state,
                        int imageNumber) {

                User user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(new User(
                                name,
                                email,
                                passwordEncoder.encode("Candidato@123"),
                                UserRole.CANDIDATE.name(),
                                true)));
                user.setActive(true);
                userRepository.save(user);

                candidateProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
                        CandidateProfile profile = new CandidateProfile(user);
                        profile.setHeadline(headline);
                        profile.setCity(city);
                        profile.setState(state);
                        profile.setProfileImageUrl("https://i.pravatar.cc/300?img=" + imageNumber);
                        return candidateProfileRepository.save(profile);
                });
        }

    private String companyImageUrl(String companyName) {
        return "https://api.dicebear.com/9.x/initials/png?seed="
                + companyName.replace(" ", "%20");
    }

    private void createGeneratedJobs(VagaRepository vagaRepository, List<CompanyProfile> companies, List<TipoDeEtapa> etapasPadrao) {
        String[] titles = {
                "Desenvolvedor(a) Full Stack", "Analista de Dados", "Engenheiro(a) de Software",
                "Especialista em Cloud", "Analista de Seguranca da Informacao", "Designer UX/UI",
                "Analista de Suporte", "Cientista de Dados", "Coordenador(a) de Projetos",
                "Consultor(a) de Negocios", "Analista Comercial", "Executivo(a) de Vendas",
                "Especialista em Marketing Digital", "Redator(a) de Conteudo", "Analista Financeiro",
                "Analista Contabil", "Analista de Recrutamento", "Especialista em Treinamento",
                "Enfermeiro(a)", "Analista de Qualidade", "Engenheiro(a) de Producao",
                "Analista de Logistica", "Coordenador(a) de Operacoes", "Tecnico(a) de Manutencao",
                "Professor(a) de Tecnologia", "Analista de Educacao Corporativa", "Arquiteto(a)",
                "Engenheiro(a) Civil", "Analista Ambiental", "Chef de Cozinha", "Recepcionista",
                "Analista de Atendimento", "Supervisor(a) de Loja", "Analista de Compras",
                "Assistente Administrativo", "Analista de Compliance", "Advogado(a) Contratual",
                "Tecnico(a) de Seguranca do Trabalho", "Analista de Pesquisa", "Especialista em Produto",
                "Product Manager", "Scrum Master", "Analista de QA", "DevOps Engineer",
                "Mobile Developer", "Estagio em Tecnologia", "Estagio em Marketing", "Estagio Administrativo",
                "Gerente de Operacoes", "Coordenador(a) de Pessoas", "Especialista em Customer Success"
        };
        VagaRole[] categories = {
                VagaRole.DESENVOLVIMENTO_SOFTWARE, VagaRole.CIENCIA_DE_DADOS_IA,
                VagaRole.DESENVOLVIMENTO_SOFTWARE, VagaRole.INFRAESTRUTURA_DEVOPS,
                VagaRole.SEGURANCA_DA_INFORMACAO, VagaRole.DESIGN_UX_UI,
                VagaRole.SUPORTE_TI, VagaRole.CIENCIA_DE_DADOS_IA, VagaRole.GESTAO_PROJETOS,
                VagaRole.CONSULTORIA_EMPRESARIAL, VagaRole.VENDAS, VagaRole.VENDAS,
                VagaRole.MARKETING_DIGITAL, VagaRole.CRIACAO_CONTEUDO, VagaRole.FINANCEIRO,
                VagaRole.CONTABILIDADE, VagaRole.RECRUTAMENTO_SELECAO, VagaRole.TREINAMENTO_DESENVOLVIMENTO,
                VagaRole.ENFERMAGEM, VagaRole.CONTROLE_QUALIDADE, VagaRole.ENGENHARIA_DE_PRODUCAO,
                VagaRole.LOGISTICA, VagaRole.ADMINISTRACAO, VagaRole.MANUTENCAO_INDUSTRIAL,
                VagaRole.DOCENCIA, VagaRole.EDUCACAO_CORPORATIVA, VagaRole.ARQUITETURA,
                VagaRole.ENGENHARIA_CIVIL, VagaRole.ENGENHARIA_AMBIENTAL, VagaRole.GASTRONOMIA,
                VagaRole.HOTELARIA, VagaRole.ATENDIMENTO_AO_CLIENTE, VagaRole.VAREJO,
                VagaRole.ADMINISTRACAO, VagaRole.COMPLIANCE, VagaRole.CONTRATOS,
                VagaRole.SEGURANCA_DO_TRABALHO, VagaRole.PESQUISA_DESENVOLVIMENTO,
                VagaRole.OUTRO, VagaRole.GESTAO_PROJETOS, VagaRole.GESTAO_PROJETOS,
                VagaRole.CONTROLE_QUALIDADE, VagaRole.INFRAESTRUTURA_DEVOPS,
                VagaRole.DESENVOLVIMENTO_SOFTWARE, VagaRole.DESENVOLVIMENTO_SOFTWARE,
                VagaRole.MARKETING_DIGITAL, VagaRole.ADMINISTRACAO, VagaRole.ADMINISTRACAO,
                VagaRole.RECURSOS_HUMANOS, VagaRole.ATENDIMENTO_AO_CLIENTE
        };

        int jobCount = Math.min(titles.length, categories.length);
        for (int index = 0; index < jobCount; index++) {
            CompanyProfile company = companies.get(index % companies.size());
            int cityIndex = index % 3;
            String city = cityIndex == 0 ? "Sao Paulo" : cityIndex == 1 ? "Campinas" : "Santos";
            Double latitude = cityIndex == 0 ? -23.5505 : cityIndex == 1 ? -22.9056 : -23.9608;
            Double longitude = cityIndex == 0 ? -46.6333 : cityIndex == 1 ? -47.0608 : -46.3336;
            ModalidadeVaga modality = ModalidadeVaga.values()[index % ModalidadeVaga.values().length];
            createJobIfAbsent(vagaRepository, company, titles[index],
                    BigDecimal.valueOf(2200L + (index % 8) * 900L),
                    "Atuar em um time colaborativo, contribuindo para resultados e melhoria continua.",
                    "Vale refeicao, plano de saude e horario flexivel.", city, "SP", latitude, longitude,
                    categories[index], modality,
                    etapasPadrao);

        }
    }

    private void createJobIfAbsent(
            VagaRepository vagaRepository,
            CompanyProfile company,
            String title,
            BigDecimal salary,
            String description,
            String benefits,
            String city,
            String state,
            Double latitude,
            Double longitude,
            VagaRole category,
            ModalidadeVaga modality,
            List<TipoDeEtapa> etapas) {

        Vaga existingJob = vagaRepository.findByEmpresaId(company.getId()).stream()
                .filter(job -> job.getTitulo().equals(title))
                .findFirst()
                .orElse(null);
        if (existingJob != null) {
            if (existingJob.getEtapas().isEmpty()) {
                existingJob.setEtapas(etapas.stream()
                        .map((etapa) -> new VagaEtapa(existingJob, etapa, etapas.indexOf(etapa) + 1))
                        .toList());
                vagaRepository.save(existingJob);
            }
            return;
        }

        Vaga job = new Vaga();
        job.setTitulo(title);
        job.setSalario(salary);
        job.setDescricao(description);
        job.setBeneficios(benefits);
        job.setEmpresa(company);
        job.setCidade(city);
        job.setEstado(state);
        job.setLatitude(latitude);
        job.setLongitude(longitude);
        job.setCategoria(category);
        job.setModalidade(modality);
        job.setAtiva(true);
        job.setEtapas(etapas.stream()
                .map((etapa) -> new VagaEtapa(job, etapa, etapas.indexOf(etapa) + 1))
                .toList());
        vagaRepository.save(job);
    }
}