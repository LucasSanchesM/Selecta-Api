package com.unisal.selectaapi.candidate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unisal.selectaapi.candidate.entity.CandidateCertificate;

@Repository
public interface CandidateCertificateRepository extends JpaRepository<CandidateCertificate, UUID> {
    Optional<CandidateCertificate> findByIdAndCandidateProfileUserId(UUID id, UUID userId);

    List<CandidateCertificate> findByCandidateProfileUserIdOrderByIssueDateDesc(UUID userId);
}
