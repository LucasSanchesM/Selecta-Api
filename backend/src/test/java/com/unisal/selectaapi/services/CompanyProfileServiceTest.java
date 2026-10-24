package com.unisal.selectaapi.services;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;

import com.unisal.selectaapi.company.dto.UpdateCompanyProfileDto;
import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.company.repository.CompanyProfileRepository;
import com.unisal.selectaapi.company.service.CompanyProfileService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.storage.FileStorage;
import com.unisal.selectaapi.storage.ImageUploadValidator;
import com.unisal.selectaapi.storage.StoredFile;
import com.unisal.selectaapi.user.entity.User;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {

    @Mock
    CompanyProfileRepository repository;

    @Mock
    FileStorage storage;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ImageUploadValidator validator;

    @InjectMocks
    CompanyProfileService service;

    UUID userId = UUID.randomUUID();

    // Simula um perfil existente
    private CompanyProfile perfil() {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);

        CompanyProfile p = new CompanyProfile(user, "Empresa Teste");
        p.setId(UUID.randomUUID());
        p.setCnpj("12345678000190");
        p.setCity("Cruzeiro");
        p.setState("SP");

        when(repository.findByUserId(userId))
                .thenReturn(Optional.of(p));
        return p;
    }

    // Simula o save sem banco de dados
    private void salvar() {
        when(repository.save(any(CompanyProfile.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    // Cria um DTO para atualizacao
    private UpdateCompanyProfileDto dto(String nome) {
        return new UpdateCompanyProfileDto(
                nome, null, null, null, null, null, null
        );
    }

    // Simula validacao e armazenamento de imagem
    private MultipartFile prepararImagem() {
        MultipartFile file = mock(MultipartFile.class);
        StoredFile stored = mock(StoredFile.class);

        when(validator.validate(file).extension()).thenReturn("png");
        when(validator.validate(file).content())
                .thenReturn(new byte[]{1, 2, 3});
        when(validator.validate(file).contentType())
                .thenReturn("image/png");

        when(stored.url()).thenReturn("https://site.com/logo.png");
        when(stored.key()).thenReturn("nova-logo");

        when(storage.store(
                anyString(), anyString(), any(), anyString()
        )).thenReturn(stored);

        return file;
    }

    @Nested
    @DisplayName("Buscar perfil")
    class BuscarPerfil {

        @Test
        void deveBuscarPerfil() {
            var p = perfil();

            var resultado = service.findProfileByUserId(userId);

            assertEquals(p.getId(), resultado.id());
            assertEquals(userId, resultado.userId());
            assertEquals("Empresa Teste", resultado.companyName());
            assertEquals("12345678000190", resultado.cnpj());
            verify(repository).findByUserId(userId);
        }

        @Test
        void deveRetornarErroSeNaoExiste() {
            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            assertThrows(BusinessException.class,
                    () -> service.findProfileByUserId(userId));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Atualizar perfil")
    class AtualizarPerfil {

        @Test
        void deveAtualizarTodosCampos() {
            var p = perfil();
            salvar();

            var entrada = new UpdateCompanyProfileDto(
                    "Nova Empresa", "98765432000110",
                    "Nova descricao", "Industria",
                    "https://nova.com", "Lorena", "SP"
            );

            var resultado = service.updateProfile(userId, entrada);

            assertEquals("Nova Empresa", resultado.companyName());
            assertEquals("98765432000110", resultado.cnpj());
            assertEquals("Nova descricao", resultado.description());
            assertEquals("Industria", resultado.industry());
            assertEquals("https://nova.com", resultado.website());
            assertEquals("Lorena", resultado.city());
            assertEquals("SP", resultado.state());

            verify(repository).save(p);
        }

        @Test
        void deveManterCamposNulos() {
            var p = perfil();
            salvar();

            var resultado = service.updateProfile(userId, dto(null));

            assertEquals("Empresa Teste", resultado.companyName());
            assertEquals("12345678000190", resultado.cnpj());
            assertEquals("Cruzeiro", resultado.city());
            verify(repository).save(p);
        }

        @Test
        void deveManterNomeSeVazio() {
            perfil();
            salvar();

            var resultado = service.updateProfile(userId, dto("   "));

            assertEquals("Empresa Teste", resultado.companyName());
        }

        @Test
        void deveRetornarErroSePerfilNaoExiste() {
            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            assertThrows(BusinessException.class,
                    () -> service.updateProfile(userId, dto("Nova")));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Upload de logo")
    class UploadLogo {

        @Test
        void deveEnviarLogo() {
            var p = perfil();
            var file = prepararImagem();
            salvar();

            var resultado = service.uploadLogo(userId, file);

            assertEquals("https://site.com/logo.png", resultado.logoUrl());
            assertEquals("nova-logo", p.getLogoKey());

            verify(storage).store(
                    eq("company-logos/" + p.getId()),
                    argThat(nome -> nome.endsWith(".png")),
                    any(),
                    eq("image/png")
            );
            verify(repository).save(p);
            verify(storage).deleteQuietly(isNull());
        }

        @Test
        void deveSubstituirLogo() {
            var p = perfil();
            p.setLogoKey("logo-antiga");
            p.setLogoUrl("https://site.com/antiga.png");

            var file = prepararImagem();
            salvar();

            var resultado = service.uploadLogo(userId, file);

            assertEquals("https://site.com/logo.png", resultado.logoUrl());
            assertEquals("nova-logo", p.getLogoKey());
            verify(storage).deleteQuietly("logo-antiga");
        }

        @Test
        void deveRetornarErroSePerfilNaoExiste() {
            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            var file = mock(MultipartFile.class);

            assertThrows(BusinessException.class,
                    () -> service.uploadLogo(userId, file));

            verifyNoInteractions(storage, validator);
        }

        @Test
        void deveRejeitarImagemInvalida() {
            perfil();
            var file = mock(MultipartFile.class);

            when(validator.validate(file))
                    .thenThrow(new IllegalArgumentException("Imagem invalida"));

            assertThrows(IllegalArgumentException.class,
                    () -> service.uploadLogo(userId, file));

            verifyNoInteractions(storage);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Excluir logo")
    class ExcluirLogo {

        @Test
        void deveExcluirLogo() {
            var p = perfil();
            p.setLogoKey("logo-antiga");
            p.setLogoUrl("https://site.com/antiga.png");
            salvar();

            var resultado = service.deleteLogo(userId);

            assertNull(resultado.logoUrl());
            assertNull(p.getLogoKey());
            verify(repository).save(p);
            verify(storage).deleteQuietly("logo-antiga");
        }

        @Test
        void deveExcluirMesmoSemLogo() {
            perfil();
            salvar();

            var resultado = service.deleteLogo(userId);

            assertNull(resultado.logoUrl());
            verify(storage).deleteQuietly(isNull());
        }

        @Test
        void deveRetornarErroSePerfilNaoExiste() {
            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            assertThrows(BusinessException.class,
                    () -> service.deleteLogo(userId));

            verify(repository, never()).save(any());
            verifyNoInteractions(storage);
        }
    }
}
