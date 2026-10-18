package com.unisal.selectaapi.candidate.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.unisal.selectaapi.candidate.dto.CandidateExperienceDto;
import com.unisal.selectaapi.candidate.dto.CandidateProfileDto;
import com.unisal.selectaapi.candidate.dto.CandidateProjectDto;
import com.unisal.selectaapi.candidate.dto.CreateExperienceDto;
import com.unisal.selectaapi.candidate.dto.CreateProfileDto;
import com.unisal.selectaapi.candidate.dto.CreateProjectDto;
import com.unisal.selectaapi.candidate.dto.ImageFormat;
import com.unisal.selectaapi.candidate.dto.UpdateCandidateProfileDto;
import com.unisal.selectaapi.candidate.dto.ValidateImageContent;
import com.unisal.selectaapi.candidate.entity.CandidateExperience;
import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.candidate.entity.CandidateProject;
import com.unisal.selectaapi.candidate.repository.CandidateExperienceRepository;
import com.unisal.selectaapi.candidate.repository.CandidateProfileRepository;
import com.unisal.selectaapi.candidate.repository.CandidateProjectRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.storage.FileStorage;
import com.unisal.selectaapi.storage.StoredFile;
import com.unisal.selectaapi.user.UserRepository;

@Service
public class CandidateProfileService {

    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;
    private static final long MAX_RESUME_SIZE = 10L * 1024 * 1024;

    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateExperienceRepository candidateExperienceRepository;
    private final CandidateProjectRepository candidateProjectRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    public CandidateProfileService(CandidateProfileRepository candidateProfileRepository,
            CandidateExperienceRepository candidateExperienceRepository,
            CandidateProjectRepository candidateProjectRepository,
            UserRepository userRepository,
            FileStorage fileStorage) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.candidateExperienceRepository = candidateExperienceRepository;
        this.candidateProjectRepository = candidateProjectRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public CandidateProfileDto createProfile(UUID userId, CreateProfileDto dto) {
        
        if (candidateProfileRepository.findByUserId(userId).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Já existe um perfil associado a este usuario");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Não existe um usuario associado a este identificador"));
        if (!"CANDIDATE".equals(user.getRole())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Apenas candidatos podem criar perfis");
        }
        
        CandidateProfile profile = new CandidateProfile(user);
        profile.setProfileImageUrl(user.getAvatarUrl());
        profile.setHeadline(dto.headline());
        profile.setSummary(dto.summary());
        profile.setPhone(dto.phone());
        profile.setCity(dto.city());
        profile.setState(dto.state());
        profile.setInstitution(dto.institution());
        profile.setCourse(dto.course());
        profile.setCurrentSemester(dto.currentSemester());
        profile.setExpectedGraduationYear(dto.expectedGraduationYear());
        profile.setLinkedinUrl(dto.linkedinUrl());
        profile.setGithubUrl(dto.githubUrl());
        profile.setPortfolioUrl(dto.portfolioUrl());
        profile.setSkills(dto.skills() != null ? new ArrayList<>(dto.skills()) : new ArrayList<>());

        if (dto.experiences() != null) {
            dto.experiences().forEach(experiencia -> profile.getExperiences().add(new CandidateExperience(profile,
                    experiencia.companyName(),
                    experiencia.role(),
                    experiencia.description(),
                    experiencia.startDate(),
                    experiencia.endDate(),
                    experiencia.isCurrent()
            )));
        }

        if (dto.projects() != null) {
            dto.projects().forEach(project -> profile.getProjects().add(new CandidateProject(profile,
                    project.title(),
                    project.description(),
                    project.projectUrl(),
                    project.toolsAndSkills()
            )));
        }

        return toDto(candidateProfileRepository.save(profile));
    }

    @Transactional
    public CandidateProfileDto findProfileByUserId(UUID userId) {
        var profile = candidateProfileRepository.findByUserId(userId);
        if (profile.isEmpty()) {
            if (userRepository.findById(userId).isEmpty()) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "Este usuario não existe");
            } else {
                throw new BusinessException(HttpStatus.NOT_FOUND, "Nenhum perfil associado a este id");
            }
        }
        return toDto(profile.get());
    }

    @Transactional
    public CandidateProfileDto updateProfile(UUID userId, UpdateCandidateProfileDto dto) {
        var profile = getProfileOrThrow(userId);

        if (dto.headline() != null) {
            profile.setHeadline(dto.headline());
        }
        if (dto.summary() != null) {
            profile.setSummary(dto.summary());
        }
        if (dto.phone() != null) {
            profile.setPhone(dto.phone());
        }
        if (dto.city() != null) {
            profile.setCity(dto.city());
        }
        if (dto.state() != null) {
            profile.setState(dto.state());
        }
        if (dto.institution() != null) {
            profile.setInstitution(dto.institution());
        }
        if (dto.course() != null) {
            profile.setCourse(dto.course());
        }
        if (dto.currentSemester() != null) {
            profile.setCurrentSemester(dto.currentSemester());
        }
        if (dto.expectedGraduationYear() != null) {
            profile.setExpectedGraduationYear(dto.expectedGraduationYear());
        }
        if (dto.linkedinUrl() != null) {
            profile.setLinkedinUrl(dto.linkedinUrl());
        }
        if (dto.githubUrl() != null) {
            profile.setGithubUrl(dto.githubUrl());
        }
        if (dto.portfolioUrl() != null) {
            profile.setPortfolioUrl(dto.portfolioUrl());
        }
        if (dto.skills() != null) {
            profile.setSkills(new ArrayList<>(dto.skills()));
        }

        CandidateProfile saved = candidateProfileRepository.save(profile);
        return toDto(saved);
    }

    @Transactional
    public CandidateProfileDto uploadAvatar(UUID userId, MultipartFile file) {
        var profile = getProfileOrThrow(userId);

        var validatedImage = readAndValidateAvatar(file);

        StoredFile uploaded = fileStorage.store(
                "candidate-avatars/" + userId,
                UUID.randomUUID() + "." + validatedImage.format().extension(),
                validatedImage.content(),
                validatedImage.format().contentType());

        String previousKey = profile.getProfileImageKey();
        profile.setProfileImageKey(uploaded.key());
        profile.setProfileImageUrl(uploaded.url());

        try {
            CandidateProfile saved = candidateProfileRepository.saveAndFlush(profile);
            fileStorage.deleteQuietly(previousKey);
            return toDto(saved);
        } catch (RuntimeException exception) {
            fileStorage.deleteQuietly(uploaded.key());
            throw exception;
        }
    }

    @Transactional
    public CandidateProfileDto uploadResume(UUID userId, MultipartFile file) {
        var profile = getProfileOrThrow(userId);

        byte[] content = readAndValidateResume(file);

        StoredFile uploaded = fileStorage.store(
                "candidate-resumes/" + userId,
                UUID.randomUUID() + ".pdf",
                content,
                "application/pdf");

        String previousKey = profile.getResumeKey();
        profile.setResumeKey(uploaded.key());
        profile.setResumeUrl(uploaded.url());
        profile.setResumeText(ResumeTextExtractor.extrair(content));


        try {
            CandidateProfile saved = candidateProfileRepository.saveAndFlush(profile);
            fileStorage.deleteQuietly(previousKey);
            return toDto(saved);
        } catch (RuntimeException exception) {
            fileStorage.deleteQuietly(uploaded.key());
            throw exception;
        }
    }

    @Transactional
    public CandidateProfileDto addExperience(UUID userId, CreateExperienceDto dto) {
        var profile = getProfileOrThrow(userId);

        CandidateExperience exp = new CandidateExperience(
                profile,
                dto.companyName(),
                dto.role(),
                dto.description(),
                dto.startDate(),
                dto.endDate(),
                dto.isCurrent());

        profile.getExperiences().add(exp);
        CandidateProfile saved = candidateProfileRepository.save(profile);
        return toDto(saved);
    }

    @Transactional
    public CandidateProfileDto updateExperience(UUID userId, UUID experienceId, CreateExperienceDto dto) {
        CandidateExperience exp = candidateExperienceRepository.findByIdAndCandidateProfileUserId(experienceId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                "Experiência não encontrada para este usuário"));

        exp.setCompanyName(dto.companyName());
        exp.setRole(dto.role());
        exp.setDescription(dto.description());
        exp.setStartDate(dto.startDate());
        exp.setEndDate(dto.endDate());
        exp.setIsCurrent(dto.isCurrent() != null ? dto.isCurrent() : false);

        candidateExperienceRepository.save(exp);
        return findProfileByUserId(userId);
    }

    @Transactional
    public CandidateProfileDto deleteExperience(UUID userId, UUID experienceId) {
        CandidateExperience exp = candidateExperienceRepository.findByIdAndCandidateProfileUserId(experienceId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                "Experiência não encontrada para este usuário"));

        CandidateProfile profile = exp.getCandidateProfile();
        profile.getExperiences().remove(exp);
        candidateExperienceRepository.delete(exp);
        CandidateProfile saved = candidateProfileRepository.save(profile);
        return toDto(saved);
    }

    @Transactional
    public CandidateProfileDto addProject(UUID userId, CreateProjectDto dto) {
        var profile = getProfileOrThrow(userId);

        CandidateProject project = new CandidateProject(
                profile,
                dto.title(),
                dto.description(),
                dto.projectUrl(),
                dto.toolsAndSkills() != null ? dto.toolsAndSkills() : new ArrayList<>());

        profile.getProjects().add(project);
        CandidateProfile saved = candidateProfileRepository.save(profile);
        return toDto(saved);
    }

    @Transactional
    public CandidateProfileDto updateProject(UUID userId, UUID projectId, CreateProjectDto dto) {
        CandidateProject project = candidateProjectRepository.findByIdAndCandidateProfileUserId(projectId, userId)
                .orElseThrow(
                        () -> new BusinessException(HttpStatus.NOT_FOUND, "Projeto não encontrado para este usuário"));

        project.setTitle(dto.title());
        project.setDescription(dto.description());
        project.setProjectUrl(dto.projectUrl());
        if (dto.toolsAndSkills() != null) {
            project.setToolsAndSkills(new ArrayList<>(dto.toolsAndSkills()));
        }

        candidateProjectRepository.save(project);
        return findProfileByUserId(userId);
    }

    @Transactional
    public CandidateProfileDto deleteProject(UUID userId, UUID projectId) {
        CandidateProject project = candidateProjectRepository.findByIdAndCandidateProfileUserId(projectId, userId)
                .orElseThrow(
                        () -> new BusinessException(HttpStatus.NOT_FOUND, "Projeto não encontrado para este usuário"));

        CandidateProfile profile = project.getCandidateProfile();
        profile.getProjects().remove(project);
        candidateProjectRepository.delete(project);
        CandidateProfile saved = candidateProfileRepository.save(profile);
        return toDto(saved);
    }

    private CandidateProfile getProfileOrThrow(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Não foi encontrado um perfil associado a este usuario"));
    }

    private ValidateImageContent readAndValidateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Selecione uma imagem de perfil");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE,
                    "A imagem deve ter no máximo 5 MB");
        }
        try {
            byte[] content = file.getBytes();
            var format = detectImageFormat(content);
            return new ValidateImageContent(content, format);
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Não foi possível ler a imagem enviada");
        }
    }

    private byte[] readAndValidateResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Selecione um arquivo PDF para o currículo");
        }
        if (file.getSize() > MAX_RESUME_SIZE) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE, "O currículo deve ter no máximo 10 MB");
        }
        try {
            byte[] content = file.getBytes();
            if (!isPdf(content)) {
                throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato inválido. O arquivo deve ser um PDF");
            }
            return content;
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado");
        }
    }

    private boolean isPdf(byte[] content) {
        return content.length >= 4
                && content[0] == (byte) 0x25
                && content[1] == (byte) 0x50
                && content[2] == (byte) 0x44
                && content[3] == (byte) 0x46;
    }

    private ImageFormat detectImageFormat(byte[] content) {
        if (isPng(content)) {
            return new ImageFormat("image/png", "png");
        }
        if (isJpeg(content)) {
            return new ImageFormat("image/jpeg", "jpg");
        }
        if (isWebp(content)) {
            return new ImageFormat("image/webp", "webp");
        }
        throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Formato inválido. Envie uma imagem PNG, JPEG ou WebP");
    }

    private boolean isPng(byte[] content) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isJpeg(byte[] content) {
        return content.length >= 3
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[2] == (byte) 0xFF;
    }

    private boolean isWebp(byte[] content) {
        return content.length >= 12
                && content[0] == 'R'
                && content[1] == 'I'
                && content[2] == 'F'
                && content[3] == 'F'
                && content[8] == 'W'
                && content[9] == 'E'
                && content[10] == 'B'
                && content[11] == 'P';
    }

    private CandidateProfileDto toDto(CandidateProfile profile) {
        var expDtos = profile.getExperiences().stream()
                .map(exp -> new CandidateExperienceDto(
                exp.getId(),
                exp.getCompanyName(),
                exp.getRole(),
                exp.getDescription(),
                exp.getStartDate(),
                exp.getEndDate(),
                exp.getIsCurrent(),
                exp.getCreatedAt()))
                .collect(Collectors.toList());

        var projDtos = profile.getProjects().stream()
                .map(proj -> new CandidateProjectDto(
                proj.getId(),
                proj.getTitle(),
                proj.getDescription(),
                proj.getProjectUrl(),
                proj.getToolsAndSkills(),
                proj.getCreatedAt()))
                .collect(Collectors.toList());

        return new CandidateProfileDto(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getName(),
                profile.getUser().getEmail(),
                profile.getProfileImageUrl(),
                profile.getResumeUrl(),
                profile.getHeadline(),
                profile.getSummary(),
                profile.getPhone(),
                profile.getCity(),
                profile.getState(),
                profile.getInstitution(),
                profile.getCourse(),
                profile.getCurrentSemester(),
                profile.getExpectedGraduationYear(),
                profile.getLinkedinUrl(),
                profile.getGithubUrl(),
                profile.getPortfolioUrl(),
                profile.getSkills(),
                expDtos,
                projDtos,
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
