package com.unisal.selectaapi.company.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.company.dto.CreateTeamMemberDto;
import com.unisal.selectaapi.company.dto.TeamMemberResponse;
import com.unisal.selectaapi.company.dto.TeamResponse;
import com.unisal.selectaapi.company.dto.UpdateTeamMemberDto;
import com.unisal.selectaapi.company.entity.CompanyMember;
import com.unisal.selectaapi.company.entity.CompanyMemberRole;
import com.unisal.selectaapi.company.repository.CompanyMemberRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserRole;
import com.unisal.selectaapi.user.entity.User;

@Service
public class CompanyTeamService {

    private final CompanyAccessService accessService;
    private final CompanyMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public CompanyTeamService(
            CompanyAccessService accessService,
            CompanyMemberRepository memberRepository,
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder) {

        this.accessService = accessService;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public TeamResponse listar(UUID userId) {
        var access = accessService.resolve(userId);
        var company = access.company();
        var owner = company.getUser();

        var membros = new ArrayList<TeamMemberResponse>();

        membros.add(new TeamMemberResponse(
                null,
                owner.getId(),
                owner.getName(),
                owner.getEmail(),
                CompanyAccess.PROPRIETARIO,
                owner.getActive(),
                owner.getId().equals(userId),
                toResponseDateTime(owner.getCreatedAt())));

        memberRepository.findAllByCompanyId(company.getId())
                .forEach(member ->
                        membros.add(toResponse(member, userId)));

        return new TeamResponse(
                access.roleName(),
                access.canManageCompany(),
                membros);
    }

    @Transactional
    public TeamMemberResponse criar(
            UUID userId,
            CreateTeamMemberDto dto) {

        var access = accessService.requireManager(userId);
        requireCanAssign(access, dto.role());

        String email = dto.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "E-mail já está em uso");
        }

        // A conta nasce ativa: quem convida já garante a identidade da pessoa.
        var user = new User(
                dto.name().trim(),
                email,
                passwordEncoder.encode(dto.password()),
                UserRole.COMPANY.name(),
                true);

        var savedUser = userRepository.save(user);

        var member = memberRepository.save(
                new CompanyMember(
                        access.company(),
                        savedUser,
                        dto.role(),
                        userId));

        return toResponse(member, userId);
    }

    @Transactional
    public TeamMemberResponse atualizar(
            UUID userId,
            UUID memberId,
            UpdateTeamMemberDto dto) {

        var access = accessService.requireManager(userId);
        var member = findEditableMember(access, memberId, userId);

        if (dto.role() != null && dto.role() != member.getRole()) {
            requireCanAssign(access, dto.role());
            member.setRole(dto.role());
        }

        var user = member.getUser();

        if (dto.active() != null) {
            user.setActive(dto.active());
        }

        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPasswordHash(
                    passwordEncoder.encode(dto.password()));
        }

        userRepository.save(user);

        return toResponse(
                memberRepository.save(member),
                userId);
    }

    @Transactional
    public void remover(UUID userId, UUID memberId) {
        var access = accessService.requireManager(userId);
        var member = findEditableMember(access, memberId, userId);
        var user = member.getUser();

        try {
            memberRepository.delete(member);
            userRepository.delete(user);
            userRepository.flush();

        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Este usuário já tem histórico na plataforma "
                            + "(mensagens, feedbacks ou etapas). "
                            + "Desative o acesso em vez de excluir.");
        }
    }

    private CompanyMember findEditableMember(
            CompanyAccess access,
            UUID memberId,
            UUID userId) {

        var member = memberRepository
                .findByIdAndCompanyId(
                        memberId,
                        access.company().getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Membro não encontrado nesta empresa"));

        if (member.getUser().getId().equals(userId)) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Você não pode alterar o próprio acesso. "
                            + "Peça a outro administrador.");
        }

        if (member.getRole() == CompanyMemberRole.ADMIN
                && !access.isOwner()) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Apenas o proprietário pode alterar "
                            + "ou remover administradores");
        }

        return member;
    }

    private void requireCanAssign(
            CompanyAccess access,
            CompanyMemberRole role) {

        if (role == CompanyMemberRole.ADMIN && !access.isOwner()) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Apenas o proprietário pode conceder "
                            + "o papel de administrador");
        }
    }

    private TeamMemberResponse toResponse(
            CompanyMember member,
            UUID currentUserId) {

        var user = member.getUser();

        return new TeamMemberResponse(
                member.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                member.getRole().name(),
                user.getActive(),
                user.getId().equals(currentUserId),
                toResponseDateTime(member.getCreatedAt()));
    }

    private LocalDateTime toResponseDateTime(Instant value) {
        return value == null
                ? null
                : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}