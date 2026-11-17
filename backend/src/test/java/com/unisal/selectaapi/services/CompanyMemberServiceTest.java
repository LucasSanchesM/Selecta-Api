
package com.unisal.selectaapi.services;

import com.unisal.selectaapi.company.dto.CreateCompanyMemberDto;
import com.unisal.selectaapi.company.dto.UpdateCompanyMemberDto;
import com.unisal.selectaapi.company.entity.CompanyMember;
import com.unisal.selectaapi.company.entity.CompanyMemberRole;
import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.company.repository.CompanyMemberRepository;
import com.unisal.selectaapi.company.repository.CompanyProfileRepository;
import com.unisal.selectaapi.company.service.CompanyMemberService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserService;
import com.unisal.selectaapi.user.entity.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyMemberServiceTest {

    // Simulamos os repositórios e serviços utilizados pelo CompanyMemberService.
    // Assim não precisamos acessar banco de dados durante os testes.
    @Mock
    CompanyProfileRepository companyProfileRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    CompanyMemberRepository companyMemberRepository;

    @Mock
    BCryptPasswordEncoder passwordEncoder;

    @Mock
    UserService userService;

    // O Mockito injeta os mocks acima no serviço que será testado.
    @InjectMocks
    CompanyMemberService service;

    // IDs fictícios para representar os diferentes usuários.
    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    // ---------------------------------------------------------
    // MÉTODOS AUXILIARES
    // ---------------------------------------------------------

    // Cria um usuário simulado.
    // O AtomicReference permite que o nome seja atualizado durante o teste.
    private User criarUsuario(UUID id, String nome, String email) {

        User user = mock(User.class);

        AtomicReference<String> nomeAtual = new AtomicReference<>(nome);

        when(user.getId()).thenReturn(id);
        when(user.getName()).thenAnswer(invocation -> nomeAtual.get());
        when(user.getEmail()).thenReturn(email);
        when(user.getActive()).thenReturn(true);

        // Simula o comportamento do setter de nome.
        doAnswer(invocation -> {
            nomeAtual.set(invocation.getArgument(0));
            return null;
        }).when(user).setName(anyString());

        return user;
    }

    // Cria uma empresa fictícia para os testes.
    private CompanyProfile criarEmpresa() {

        CompanyProfile company = mock(CompanyProfile.class);

        when(company.getId()).thenReturn(UUID.randomUUID());

        return company;
    }

    // Cria um CompanyMember vinculado a uma empresa e usuário.
    private CompanyMember criarMembro(
            UUID userId,
            CompanyMemberRole role,
            CompanyProfile company
    ) {

        CompanyMember member = new CompanyMember();

        member.setId(UUID.randomUUID());
        member.setUser(criarUsuario(userId, "Usuário", userId + "@email.com"));
        member.setCompany(company);
        member.setRole(role);
        member.setCreatedBy(ownerId);

        return member;
    }

    // Configura um usuário que não é proprietário,
    // mas está associado a uma empresa como membro.
    private void configurarMembro(UUID userId, CompanyMember member) {

        when(companyProfileRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        when(companyMemberRepository.findByUserId(userId))
                .thenReturn(Optional.of(member));
    }

    // ---------------------------------------------------------
    // 1. LISTAR MEMBROS
    // ---------------------------------------------------------

    @Test
    @DisplayName("Dono consegue listar os membros")
    void donoListaMembros() {

        // Arrange: preparamos o usuário proprietário e um membro.
        User owner = criarUsuario(ownerId, "Dono", "owner@email.com");

        CompanyMember member = criarMembro(
                memberId,
                CompanyMemberRole.RECRUTADOR,
                criarEmpresa()
        );

        when(userRepository.findById(ownerId))
                .thenReturn(Optional.of(owner));

        when(companyProfileRepository.existsByUserId(ownerId))
                .thenReturn(true);

        when(companyMemberRepository.findAllByCompany_User_Id(ownerId))
                .thenReturn(List.of(member));

        // Act: executamos o método que queremos testar.
        var resultado = service.listarMembros(ownerId);

        // Assert: verificamos se o proprietário pode gerenciar
        // e se o membro foi retornado.
        assertTrue(resultado.podeGerencias());
        assertEquals(1, resultado.companyMembersDto().size());
        assertEquals(memberId, resultado.companyMembersDto().get(0).userId());

        verify(companyMemberRepository)
                .findAllByCompany_User_Id(ownerId);
    }

    @Test
    @DisplayName("Membro comum não pode gerenciar")
    void membroComumNaoPodeGerenciar() {

        User user = criarUsuario(memberId, "Membro", "member@email.com");

        CompanyMember member = criarMembro(
                memberId,
                CompanyMemberRole.RECRUTADOR,
                criarEmpresa()
        );

        when(userRepository.findById(memberId))
                .thenReturn(Optional.of(user));

        when(companyProfileRepository.existsByUserId(memberId))
                .thenReturn(false);

        when(companyMemberRepository.existsByUserId(memberId))
                .thenReturn(true);

        when(companyMemberRepository.findByUserId(memberId))
                .thenReturn(Optional.of(member));

        when(companyMemberRepository.pegarMembrosDaMesmaEmpresa(memberId))
                .thenReturn(List.of(member));

        var resultado = service.listarMembros(memberId);

        // Um membro comum pode visualizar, mas não gerenciar.
        assertFalse(resultado.podeGerencias());

        // Verificamos também se ele aparece na lista.
        assertEquals(1, resultado.companyMembersDto().size());
        assertTrue(resultado.companyMembersDto().get(0).voce());
    }

    @Test
    @DisplayName("Usuário inexistente não consegue listar")
    void usuarioInexistenteNaoLista() {

        // Simulamos que o usuário não existe no banco.
        when(userRepository.findById(memberId))
                .thenReturn(Optional.empty());

        // Esperamos que o serviço lance uma BusinessException.
        assertThrows(
                BusinessException.class,
                () -> service.listarMembros(memberId)
        );
    }

    // ---------------------------------------------------------
    // 2. CRIAR MEMBRO
    // ---------------------------------------------------------

    @Test
    @DisplayName("Dono consegue criar membro")
    void donoCriaMembro() {

        User owner = criarUsuario(ownerId, "Dono", "owner@email.com");

        CompanyProfile company = criarEmpresa();

        // Simulamos que o usuário é proprietário.
        when(userRepository.findById(ownerId))
                .thenReturn(Optional.of(owner));

        when(companyProfileRepository.existsByUserId(ownerId))
                .thenReturn(true);

        // O e-mail ainda não está cadastrado.
        when(userRepository.findByEmail("novo@email.com"))
                .thenReturn(Optional.empty());

        // Simulamos a criptografia da senha.
        when(passwordEncoder.encode("123456"))
                .thenReturn("senha-criptografada");

        // Simulamos o salvamento do usuário.
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // O proprietário possui uma empresa.
        when(companyProfileRepository.findByUserId(ownerId))
                .thenReturn(Optional.of(company));

        // Simulamos o salvamento do vínculo com a empresa.
        when(companyMemberRepository.save(any(CompanyMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var dto = new CreateCompanyMemberDto(
                "Novo",
                "novo@email.com",
                "123456",
                CompanyMemberRole.RECRUTADOR
        );

        var resultado = service.criarMembro(ownerId, dto);

        // Conferimos os dados retornados pela DTO.
        assertEquals("Novo", resultado.name());
        assertEquals("novo@email.com", resultado.email());
        assertEquals(CompanyMemberRole.RECRUTADOR.name(), resultado.role());
        assertTrue(resultado.active());
        assertFalse(resultado.voce());

        // A senha deve ser criptografada antes do salvamento.
        verify(passwordEncoder).encode("123456");

        verify(userRepository).save(any(User.class));
        verify(companyMemberRepository).save(any(CompanyMember.class));
    }

    @Test
    @DisplayName("Não permite criar membro com e-mail duplicado")
    void emailDuplicadoNaoCriaMembro() {

        User owner = criarUsuario(ownerId, "Dono", "owner@email.com");

        when(userRepository.findById(ownerId))
                .thenReturn(Optional.of(owner));

        when(companyProfileRepository.existsByUserId(ownerId))
                .thenReturn(true);

        // Simulamos que o e-mail já pertence a outro usuário.
        when(userRepository.findByEmail("novo@email.com"))
                .thenReturn(Optional.of(
                        criarUsuario(
                                UUID.randomUUID(),
                                "Existente",
                                "novo@email.com"
                        )
                ));

        var dto = new CreateCompanyMemberDto(
                "Novo",
                "novo@email.com",
                "123456",
                CompanyMemberRole.RECRUTADOR
        );

        // O serviço deve impedir a criação.
        assertThrows(
                BusinessException.class,
                () -> service.criarMembro(ownerId, dto)
        );

        // Não pode salvar um novo usuário.
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Membro comum não pode criar outros membros")
    void membroComumNaoCriaMembro() {

        User user = criarUsuario(memberId, "Membro", "member@email.com");

        CompanyMember member = criarMembro(
                memberId,
                CompanyMemberRole.RECRUTADOR,
                criarEmpresa()
        );

        when(userRepository.findById(memberId))
                .thenReturn(Optional.of(user));

        when(companyProfileRepository.existsByUserId(memberId))
                .thenReturn(false);

        when(companyMemberRepository.findByUserId(memberId))
                .thenReturn(Optional.of(member));

        var dto = new CreateCompanyMemberDto(
                "Novo",
                "novo@email.com",
                "123456",
                CompanyMemberRole.RECRUTADOR
        );

        // Esperamos uma exceção por falta de permissão.
        assertThrows(
                BusinessException.class,
                () -> service.criarMembro(memberId, dto)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    // ---------------------------------------------------------
    // 3. ATUALIZAR INFORMAÇÕES
    // ---------------------------------------------------------

    @Test
    @DisplayName("Usuário consegue atualizar seu próprio nome")
    void usuarioAtualizaProprioNome() {

        CompanyProfile company = criarEmpresa();

        CompanyMember member = criarMembro(
                memberId,
                CompanyMemberRole.RECRUTADOR,
                company
        );

        configurarMembro(memberId, member);

        var dto = new UpdateCompanyMemberDto(
                "Nome novo",
                null,
                null
        );

        // O usuário está atualizando suas próprias informações.
        var resultado = service.updateInfos(memberId, dto, memberId);

        assertEquals("Nome novo", resultado.name());

        // Confirmamos que o setter de nome foi utilizado.
        verify(member.getUser()).setName("Nome novo");
    }

    @Test
    @DisplayName("Dono consegue alterar role de membro")
    void donoAlteraRole() {

        CompanyProfile company = criarEmpresa();

        CompanyMember member = criarMembro(
                memberId,
                CompanyMemberRole.RECRUTADOR,
                company
        );

        configurarMembro(memberId, member);

        // Simulamos que o atualizador é o proprietário da empresa.
        when(companyProfileRepository.findByUserId(ownerId))
                .thenReturn(Optional.of(company));

        when(companyProfileRepository.existsByUserId(ownerId))
                .thenReturn(true);

        var dto = new UpdateCompanyMemberDto(
                null,
                null,
                CompanyMemberRole.ADMIN
        );

        var resultado = service.updateInfos(memberId, dto, ownerId);

        // A role deve mudar de MEMBER para ADMIN.
        assertEquals(CompanyMemberRole.ADMIN.name(), resultado.role());
    }

    @Test
    @DisplayName("Administrador não pode alterar role")
    void adminNaoAlteraRole() {

        CompanyProfile company = criarEmpresa();

        CompanyMember member = criarMembro(
                memberId,
                CompanyMemberRole.RECRUTADOR,
                company
        );

        CompanyMember admin = criarMembro(
                adminId,
                CompanyMemberRole.ADMIN,
                company
        );

        configurarMembro(memberId, member);
        configurarMembro(adminId, admin);

        // O administrador não é proprietário.
        when(companyProfileRepository.existsByUserId(adminId))
                .thenReturn(false);

        var dto = new UpdateCompanyMemberDto(
                null,
                null,
                CompanyMemberRole.ADMIN
        );

        // A alteração deve ser bloqueada.
        assertThrows(
                BusinessException.class,
                () -> service.updateInfos(memberId, dto, adminId)
        );

        // A role do membro continua sendo MEMBER.
        assertEquals(CompanyMemberRole.RECRUTADOR, member.getRole());
    }

    // ---------------------------------------------------------
    // 4. EXCLUIR MEMBRO
    // ---------------------------------------------------------

    @Test
    @DisplayName("Dono consegue excluir membro")
    void donoExcluiMembro() {

        CompanyProfile company = criarEmpresa();

        CompanyMember member = criarMembro(
                memberId,
                CompanyMemberRole.RECRUTADOR,
                company
        );

        configurarMembro(memberId, member);

        // O dono possui a empresa do membro.
        when(companyProfileRepository.findByUserId(ownerId))
                .thenReturn(Optional.of(company));

        when(companyProfileRepository.existsByUserId(ownerId))
                .thenReturn(true);

        service.deleteMemberCompany(memberId, ownerId);

        // O vínculo e o usuário devem ser excluídos.
        verify(companyMemberRepository).delete(member);
        verify(userService).deleteUser(memberId);
    }

    @Test
    @DisplayName("Administrador não pode excluir outro administrador")
    void adminNaoExcluiAdmin() {

        CompanyProfile company = criarEmpresa();

        CompanyMember target = criarMembro(
                memberId,
                CompanyMemberRole.ADMIN,
                company
        );

        CompanyMember admin = criarMembro(
                adminId,
                CompanyMemberRole.ADMIN,
                company
        );

        configurarMembro(memberId, target);
        configurarMembro(adminId, admin);

        when(companyProfileRepository.existsByUserId(adminId))
                .thenReturn(false);

        // Administrador não pode excluir outro administrador.
        assertThrows(
                BusinessException.class,
                () -> service.deleteMemberCompany(memberId, adminId)
        );

        // Nenhuma exclusão deve acontecer.
        verify(companyMemberRepository, never())
                .delete(any(CompanyMember.class));

        verify(userService, never())
                .deleteUser(any(UUID.class));
    }

    @Test
    @DisplayName("Membro comum não pode excluir outro membro")
    void membroComumNaoExcluiMembro() {

        CompanyProfile company = criarEmpresa();

        CompanyMember target = criarMembro(
                memberId,
                CompanyMemberRole.RECRUTADOR,
                company
        );

        CompanyMember outroMembro = criarMembro(
                adminId,
                CompanyMemberRole.RECRUTADOR,
                company
        );

        configurarMembro(memberId, target);
        configurarMembro(adminId, outroMembro);

        when(companyProfileRepository.existsByUserId(adminId))
                .thenReturn(false);

        assertThrows(
                BusinessException.class,
                () -> service.deleteMemberCompany(memberId, adminId)
        );

        // O membro comum não deve excluir ninguém.
        verify(companyMemberRepository, never())
                .delete(any(CompanyMember.class));

        verify(userService, never())
                .deleteUser(any(UUID.class));
    }

    // ---------------------------------------------------------
    // 5. BUSCAR EMPRESA
    // ---------------------------------------------------------

    @Test
    @DisplayName("Busca empresa do proprietário")
    void buscaEmpresaDoDono() {

        CompanyProfile company = criarEmpresa();

        when(companyProfileRepository.findByUserId(ownerId))
                .thenReturn(Optional.of(company));

        var resultado = service.pegarCompanyPeloUserId(ownerId);

        assertEquals(company, resultado);

        // Se o usuário é proprietário, não precisa buscar CompanyMember.
        verifyNoInteractions(companyMemberRepository);
    }

    @Test
    @DisplayName("Busca empresa do membro")
    void buscaEmpresaDoMembro() {

        CompanyProfile company = criarEmpresa();

        CompanyMember member = criarMembro(
                memberId,
                CompanyMemberRole.RECRUTADOR,
                company
        );

        // O usuário não é proprietário, mas está associado à empresa.
        when(companyProfileRepository.findByUserId(memberId))
                .thenReturn(Optional.empty());

        when(companyMemberRepository.findByUserId(memberId))
                .thenReturn(Optional.of(member));

        var resultado = service.pegarCompanyPeloUserId(memberId);

        assertEquals(company, resultado);
    }

    @Test
    @DisplayName("Usuário sem empresa gera erro")
    void usuarioSemEmpresa() {

        // Não existe perfil de empresa nem vínculo de membro.
        when(companyProfileRepository.findByUserId(memberId))
                .thenReturn(Optional.empty());

        when(companyMemberRepository.findByUserId(memberId))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> service.pegarCompanyPeloUserId(memberId)
        );
    }
}
