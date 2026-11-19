package com.unisal.selectaapi.candidate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unisal.selectaapi.candidate.entity.CandidateLanguage;

@Repository
public interface CandidateLanguageRepository extends JpaRepository<CandidateLanguage, UUID> {
    Optional<CandidateLanguage> findByIdAndCandidateProfileUserId(UUID id, UUID userId);

    List<CandidateLanguage> findByCandidateProfileUserIdOrderByLanguageAsc(UUID userId);

    boolean existsByCandidateProfileUserIdAndLanguageIgnoreCase(UUID userId, String language);

    boolean existsByCandidateProfileUserIdAndLanguageIgnoreCaseAndIdNot(UUID userId, String language, UUID id);
}
