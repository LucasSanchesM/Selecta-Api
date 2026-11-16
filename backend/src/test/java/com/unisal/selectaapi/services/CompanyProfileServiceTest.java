
package com.unisal.selectaapi.services;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;

import com.unisal.selectaapi.company.dto.CompanyProfileDto;
import com.unisal.selectaapi.company.dto.UpdateCompanyProfileDto;
import com.unisal.selectaapi.company.entity.CompanyMember;
import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.company.repository.CompanyMemberRepository;
import com.unisal.selectaapi.company.repository.CompanyProfileRepository;
import com.unisal.selectaapi.company.service.CompanyProfileService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.storage.FileStorage;
import com.unisal.selectaapi.storage.ImageUploadValidator;
import com.unisal.selectaapi.storage.StoredFile;
import com.unisal.selectaapi.user.entity.User;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {

    // Simulam as dependências do serviço.
    // Assim conseguimos testar sem acessar banco ou armazenamento real.
    @Mock
    CompanyProfileRepository repository;

    @Mock
    CompanyMemberRepository companyMemberRepository;

    @Mock
    FileStorage storage;

    // O validador retorna um objeto com informações da imagem.
    // Deep stubs permitem simular esse retorno sem precisar criar
    // um objeto real do resultado da validação.
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ImageUploadValidator validator;

    // O Mockito monta o serviço utilizando os mocks acima.
    @InjectMocks
    CompanyProfileService service;

    // Utilizamos o mesmo usuário durante os testes desta classe.
    UUID userId = UUID.randomUUID();

    // Cria uma empresa de exemplo para reaproveitar nos testes.
    private CompanyProfile perfil() {

        User user = mock(User.class);

        when(user.getId()).thenReturn(userId);

        CompanyProfile profile = new CompanyProfile(user, "Empresa Teste");

        profile.setId(UUID.randomUUID());
        profile.setCnpj("12345678000190");
        profile.setCity("Cruzeiro");
        profile.setState("SP");

        // Sempre que o serviço procurar o perfil deste usuário,
        // encontrará a empresa criada aqui.
        when(repository.findByUserId(userId))
                .thenReturn(Optional.of(profile));

        return profile;
    }

    // Simula o save do banco devolvendo o próprio objeto recebido.
    // Isso evita precisar de uma conexão real com o banco de dados.
    private void salvar() {

        when(repository.save(any(CompanyProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // Cria uma atualização simples, permitindo testar campos nulos
    // ou nome vazio sem repetir a construção do DTO.
    private UpdateCompanyProfileDto dto(String nome) {

        return new UpdateCompanyProfileDto(
                nome,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    // Prepara uma imagem válida para os testes de upload.
    // O arquivo não é real: tudo é simulado pelo Mockito.
    private MultipartFile prepararImagem() {

        MultipartFile file = mock(MultipartFile.class);
        StoredFile stored = mock(StoredFile.class);

        when(validator.validate(file).extension())
                .thenReturn("png");

        when(validator.validate(file).content())
                .thenReturn(new byte[]{1, 2, 3});

        when(validator.validate(file).contentType())
                .thenReturn("image/png");

        when(stored.url())
                .thenReturn("https://site.com/logo.png");

        when(stored.key())
                .thenReturn("nova-logo");

        when(storage.store(
                anyString(),
                anyString(),
                any(),
                anyString()
        )).thenReturn(stored);

        return file;
    }

    // ============================================================
    // BUSCAR PERFIL
    // ============================================================

    @Nested
    @DisplayName("Buscar perfil")
    class BuscarPerfil {

        @Test
        @DisplayName("Deve encontrar o perfil do usuário")
        void deveBuscarPerfil() {

            var profile = perfil();

            var resultado = service.findProfileByUserId(userId);

            // Confere se os dados principais foram retornados corretamente.
            assertEquals(profile.getId(), resultado.id());
            assertEquals(userId, resultado.userId());
            assertEquals("Empresa Teste", resultado.companyName());
            assertEquals("12345678000190", resultado.cnpj());
            assertEquals("Cruzeiro", resultado.city());

            verify(repository).findByUserId(userId);
        }

        @Test
        @DisplayName("Deve retornar erro quando o perfil não existe")
        void deveRetornarErroSeNaoExiste() {

            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            // Sem perfil, o serviço deve lançar uma exceção de negócio.
            assertThrows(
                    BusinessException.class,
                    () -> service.findProfileByUserId(userId)
            );

            // Uma busca não deve tentar salvar nada.
            verify(repository, never()).save(any());
        }
    }

    // ============================================================
    // BUSCAR EMPRESA DO USUÁRIO
    // ============================================================

    @Nested
    @DisplayName("Buscar empresa do usuário")
    class BuscarEmpresaDoUsuario {

        @Test
        @DisplayName("Deve encontrar a empresa do proprietário")
        void deveBuscarEmpresaDoProprietario() {

            var profile = perfil();

            var resultado = service.findCompanyOfUser(userId);

            assertEquals(profile.getId(), resultado.id());
            assertEquals("Empresa Teste", resultado.companyName());
            assertEquals(userId, resultado.userId());

            // Se o usuário é dono, não precisa procurar vínculo de membro.
            verify(repository).findByUserId(userId);
            verifyNoInteractions(companyMemberRepository);
        }

        @Test
        @DisplayName("Deve encontrar a empresa vinculada ao membro")
        void deveBuscarEmpresaDoMembro() {

            // Neste caso, o usuário não possui perfil próprio.
            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            // Criamos uma empresa e vinculamos o usuário a ela.
            var profile = perfil();
            
            // O perfil criado acima configura a busca do proprietário.
            // Para simular um membro, removemos esse resultado.
            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            CompanyMember member = mock(CompanyMember.class);

            when(member.getCompany()).thenReturn(profile);

            when(companyMemberRepository.findByUserId(userId))
                    .thenReturn(Optional.of(member));

            var resultado = service.findCompanyOfUser(userId);

            assertEquals(profile.getId(), resultado.id());
            assertEquals("Empresa Teste", resultado.companyName());

            verify(companyMemberRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("Deve retornar erro quando o usuário não possui empresa")
        void deveRetornarErroSeUsuarioNaoTemEmpresa() {

            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            when(companyMemberRepository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            assertThrows(
                    BusinessException.class,
                    () -> service.findCompanyOfUser(userId)
            );

            verify(repository).findByUserId(userId);
            verify(companyMemberRepository).findByUserId(userId);
        }
    }

    // ============================================================
    // ATUALIZAR PERFIL
    // ============================================================

    @Nested
    @DisplayName("Atualizar perfil")
    class AtualizarPerfil {

        @Test
        @DisplayName("Deve atualizar todos os campos")
        void deveAtualizarTodosCampos() {

            var profile = perfil();
            salvar();

            var entrada = new UpdateCompanyProfileDto(
                    "Nova Empresa",
                    "98765432000110",
                    "Nova descricao",
                    "Industria",
                    "https://nova.com",
                    "Lorena",
                    "SP"
            );

            var resultado = service.updateProfile(userId, entrada);

            // Confere se todas as alterações chegaram ao resultado.
            assertEquals("Nova Empresa", resultado.companyName());
            assertEquals("98765432000110", resultado.cnpj());
            assertEquals("Nova descricao", resultado.description());
            assertEquals("Industria", resultado.industry());
            assertEquals("https://nova.com", resultado.website());
            assertEquals("Lorena", resultado.city());
            assertEquals("SP", resultado.state());

            verify(repository).save(profile);
        }

        @Test
        @DisplayName("Deve manter os campos que não foram enviados")
        void deveManterCamposNulos() {

            var profile = perfil();
            salvar();

            // Quando os campos são nulos, o serviço não deve apagá-los.
            var resultado = service.updateProfile(userId, dto(null));

            assertEquals("Empresa Teste", resultado.companyName());
            assertEquals("12345678000190", resultado.cnpj());
            assertEquals("Cruzeiro", resultado.city());
            assertEquals("SP", resultado.state());

            verify(repository).save(profile);
        }

        @Test
        @DisplayName("Deve manter o nome quando ele estiver vazio")
        void deveManterNomeSeVazio() {

            perfil();
            salvar();

            var resultado = service.updateProfile(userId, dto("   "));

            // Nome em branco não deve substituir o nome atual.
            assertEquals("Empresa Teste", resultado.companyName());
        }

        @Test
        @DisplayName("Deve retornar erro quando o perfil não existe")
        void deveRetornarErroSePerfilNaoExiste() {

            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            assertThrows(
                    BusinessException.class,
                    () -> service.updateProfile(userId, dto("Nova"))
            );

            // Não existe perfil para atualizar ou salvar.
            verify(repository, never()).save(any());
        }
    }

    // ============================================================
    // ENVIAR LOGO
    // ============================================================

    @Nested
    @DisplayName("Upload de logo")
    class UploadLogo {

        @Test
        @DisplayName("Deve enviar uma nova logo")
        void deveEnviarLogo() {

            var profile = perfil();
            var file = prepararImagem();
            salvar();

            var resultado = service.uploadLogo(userId, file);

            assertEquals(
                    "https://site.com/logo.png",
                    resultado.logoUrl()
            );

            assertEquals("nova-logo", profile.getLogoKey());

            // A imagem deve ser armazenada no diretório da empresa.
            verify(storage).store(
                    eq("company-logos/" + profile.getId()),
                    argThat(nome -> nome.endsWith(".png")),
                    any(),
                    eq("image/png")
            );

            verify(validator).validate(file);
            verify(repository).save(profile);

            // Como não havia logo anterior, o serviço recebe null
            // na tentativa de excluir a antiga.
            verify(storage).deleteQuietly(isNull());
        }

        @Test
        @DisplayName("Deve substituir a logo antiga")
        void deveSubstituirLogo() {

            var profile = perfil();

            profile.setLogoKey("logo-antiga");
            profile.setLogoUrl("https://site.com/antiga.png");

            var file = prepararImagem();
            salvar();

            var resultado = service.uploadLogo(userId, file);

            assertEquals(
                    "https://site.com/logo.png",
                    resultado.logoUrl()
            );

            assertEquals("nova-logo", profile.getLogoKey());

            // Depois de salvar a nova logo, a antiga deve ser excluída.
            verify(storage).deleteQuietly("logo-antiga");
        }

        @Test
        @DisplayName("Deve retornar erro se o perfil não existe")
        void deveRetornarErroSePerfilNaoExiste() {

            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            var file = mock(MultipartFile.class);

            assertThrows(
                    BusinessException.class,
                    () -> service.uploadLogo(userId, file)
            );

            // Sem perfil, não faz sentido validar ou armazenar imagem.
            verifyNoInteractions(storage, validator);
        }

        @Test
        @DisplayName("Deve rejeitar uma imagem inválida")
        void deveRejeitarImagemInvalida() {

            perfil();

            var file = mock(MultipartFile.class);

            when(validator.validate(file))
                    .thenThrow(new IllegalArgumentException("Imagem invalida"));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.uploadLogo(userId, file)
            );

            // Uma imagem inválida não deve chegar ao armazenamento.
            verify(validator).validate(file);
            verifyNoInteractions(storage);
            verify(repository, never()).save(any());
        }
    }

    // ============================================================
    // EXCLUIR LOGO
    // ============================================================

    @Nested
    @DisplayName("Excluir logo")
    class ExcluirLogo {

        @Test
        @DisplayName("Deve excluir a logo existente")
        void deveExcluirLogo() {

            var profile = perfil();

            profile.setLogoKey("logo-antiga");
            profile.setLogoUrl("https://site.com/antiga.png");

            salvar();

            var resultado = service.deleteLogo(userId);

            assertNull(resultado.logoUrl());
            assertNull(profile.getLogoKey());

            verify(repository).save(profile);
            verify(storage).deleteQuietly("logo-antiga");
        }

        @Test
        @DisplayName("Deve funcionar mesmo quando não existe logo")
        void deveExcluirMesmoSemLogo() {

            perfil();
            salvar();

            var resultado = service.deleteLogo(userId);

            // O perfil continua sem logo depois da operação.
            assertNull(resultado.logoUrl());

            // Comportamento atual do serviço: tenta excluir utilizando null.
            verify(storage).deleteQuietly(isNull());
        }

        @Test
        @DisplayName("Deve retornar erro quando o perfil não existe")
        void deveRetornarErroSePerfilNaoExiste() {

            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            assertThrows(
                    BusinessException.class,
                    () -> service.deleteLogo(userId)
            );

            // Sem perfil, não deve salvar nem mexer no armazenamento.
            verify(repository, never()).save(any());
            verifyNoInteractions(storage);
        }
    }

}
