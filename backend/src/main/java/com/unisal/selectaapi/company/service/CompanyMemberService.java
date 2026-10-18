package com.unisal.selectaapi.company.service;

import com.unisal.selectaapi.company.dto.CompanyMembersDto;
import com.unisal.selectaapi.company.dto.CompanyMemberDto;
import com.unisal.selectaapi.company.dto.CreateCompanyMemberDto;
import com.unisal.selectaapi.company.dto.UpdateCompanyMemberDto;
import com.unisal.selectaapi.company.entity.CompanyMember;
import com.unisal.selectaapi.company.entity.CompanyMemberRole;
import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.company.repository.CompanyMemberRepository;
import com.unisal.selectaapi.company.repository.CompanyProfileRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserRole;
import com.unisal.selectaapi.user.UserService;
import com.unisal.selectaapi.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyMemberService {

    private final CompanyProfileRepository companyProfileRepository;
    private final UserRepository userRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserService userService;

    public CompanyMemberService(CompanyProfileRepository companyProfileRepository, UserRepository userRepository, CompanyMemberRepository companyMemberRepository, BCryptPasswordEncoder passwordEncoder, UserService userService) {
        this.companyProfileRepository = companyProfileRepository;
        this.userRepository = userRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public CompanyMembersDto listarMembros(UUID userId){
        var user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Este usuário nao existe"));
        var ehDono = companyProfileRepository.existsByUserId(userId);
        List<CompanyMember> listaMembros;

        if(ehDono){
            listaMembros = companyMemberRepository.findAllByCompany_User_Id(userId);
        }
        else{
            if(!companyMemberRepository.existsByUserId(userId)){
                throw new BusinessException(HttpStatus.NOT_FOUND, "Este companyMember não existe");
            }
            listaMembros = companyMemberRepository.pegarMembrosDaMesmaEmpresa(userId);
        }

        var podeGerenciar = ehDono || verificarSePodeGerenciar(user.getId());

        return new CompanyMembersDto(podeGerenciar, toDtoMembers(listaMembros, user, ehDono));
    }

    @Transactional
    public CompanyMemberDto criarMembro(UUID userId, CreateCompanyMemberDto dto){
        var userRequest = userRepository.findById(userId).orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Este usuario nao existe"));
        var ehDono = companyProfileRepository.existsByUserId(userId);
        var podeGerenciar =  ehDono || verificarSePodeGerenciar(userRequest.getId());
        if(!podeGerenciar){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Este usuario nao pode criar outros membros da company");
        }
        if(userRepository.findByEmail(dto.email()).isPresent()){
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "Este e-mail ja está em uso");
        }

        if(!ehDono && CompanyMemberRole.ADMIN.equals(dto.role())){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Apenas o dono da empresa pode criar outros usuarios admin");
        }

        var newUser = new User();
        newUser.setName(dto.name());
        newUser.setEmail(dto.email());
        newUser.setPasswordHash(passwordEncoder.encode(dto.password()));
        newUser.setRole(UserRole.COMPANY.name());
        newUser.setActive(true);
        var saved = userRepository.save(newUser);

        var company = pegarCompanyPeloUserId(userId);

        var newCompanyMember = new CompanyMember();
        newCompanyMember.setCompany(company);
        newCompanyMember.setRole(dto.role());
        newCompanyMember.setUser(saved);
        newCompanyMember.setCreatedBy(userId);

        var memberSaved = companyMemberRepository.save(newCompanyMember);
        return toDtoMember(memberSaved, false);

    }

    @Transactional
    public CompanyMemberDto updateInfos(UUID userId, UpdateCompanyMemberDto dto, UUID atualizador){
        var companyMember = companyMemberRepository.findByUserId(userId)
                                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Este companyMember nao existe"));
        var user = companyMember.getUser();

        verificarSeDoisUsuariosSaoDaMesmaEmpresa(userId, atualizador);
        boolean atualizadorEhVoce = userId.equals(atualizador);
        boolean atualizadorEhDono = companyProfileRepository.existsByUserId(atualizador);
        boolean atualizadorEhAdmin = !atualizadorEhDono && verificarSePodeGerenciar(atualizador);

        boolean alvoEhAdmin = CompanyMemberRole.ADMIN.equals(companyMember.getRole());
        boolean mudarARole = dto.role() != null && !dto.role().equals(companyMember.getRole());

        //Aqui a gente valida as permissoes
        if(!atualizadorEhVoce && !atualizadorEhAdmin && !atualizadorEhDono){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Você nao possui permissão para atualizar estas informações");
        }
        if(alvoEhAdmin && !atualizadorEhDono && !atualizadorEhVoce){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Administradores não podem atualizar outros administradores");
        }
        if(mudarARole && !atualizadorEhDono){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Apenas o dono pode alterar a role");
        }



        if(dto.name() != null && !dto.name().isBlank()){
            user.setName(dto.name());
        }
        if(dto.password() != null && !dto.password().isBlank()){
            user.setPasswordHash(passwordEncoder.encode(dto.password()));
        }
        if(mudarARole){
            companyMember.setRole(dto.role());
        }

        return toDtoMember(companyMember, atualizadorEhVoce);
    }

    @Transactional
    public void deleteMemberCompany(UUID userId, UUID deletador){
        var companyMember = companyMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Este companyMember nao existe"));

        verificarSeDoisUsuariosSaoDaMesmaEmpresa(userId, deletador);
        boolean deletadorEHDono = companyProfileRepository.existsByUserId(deletador);
        boolean deletadorEhAdmin = !deletadorEHDono && verificarSePodeGerenciar(deletador);

        boolean alvoEhAdmin = CompanyMemberRole.ADMIN.equals(companyMember.getRole());

        if(alvoEhAdmin && !deletadorEHDono){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Apenas o dono pode deletar administradores");
        }
        if(!deletadorEhAdmin && !deletadorEHDono){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Apenas administradores ou o dono podem deletar membros");
        }

        companyMemberRepository.delete(companyMember);
        userService.deleteUser(userId);

    }

    private void verificarSeDoisUsuariosSaoDaMesmaEmpresa(UUID userId1, UUID userId2){
        if(!pegarCompanyPeloUserId(userId1).getId().equals(pegarCompanyPeloUserId(userId2).getId())){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Apenas pessoas da mesma empresa podem interarir em alterações");
        }
    }

    public CompanyProfile pegarCompanyPeloUserId(UUID userId){
        var company = companyProfileRepository.findByUserId(userId);
        if(company.isEmpty()){
            var companyMember = companyMemberRepository.findByUserId(userId).orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Este usuário não está associado a nenhuma empresa"));
            return companyMember.getCompany();
        }
        return company.get();
    }

    private boolean verificarSePodeGerenciar(UUID userId){
        var companyMember = companyMemberRepository.findByUserId(userId)
                                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Membro nao encontrado"));
        return CompanyMemberRole.ADMIN.equals(companyMember.getRole());
    }

    private boolean verificarSeMembroDaCompanyEhVoce(User user, CompanyMember companyMember){
        return companyMember.getUser().getId().equals(user.getId());
    }

    private CompanyMemberDto toDtoMember(CompanyMember companyMember, boolean ehVoce){
        return new CompanyMemberDto(
                companyMember.getId(),
                companyMember.getUser().getId(),
                companyMember.getUser().getName(),
                companyMember.getUser().getEmail(),
                companyMember.getRole().name(),
                companyMember.getUser().getActive(),
                ehVoce,
                companyMember.getCreatedAt()
        );
    }

    private List<CompanyMemberDto> toDtoMembers(List<CompanyMember> lista, User user, boolean ehDono){
        return lista.stream().map(registro ->
                toDtoMember(registro, !ehDono && verificarSeMembroDaCompanyEhVoce(user, registro)
                )).toList();

    }
}
