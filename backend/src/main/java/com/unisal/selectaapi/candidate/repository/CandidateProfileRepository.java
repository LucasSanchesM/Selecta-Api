package com.unisal.selectaapi.candidate.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.unisal.selectaapi.candidate.entity.CandidateProfile;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    Optional<CandidateProfile> findByUserId(UUID userId);

    @Query(
            value = """
            SELECT EXISTS(
                        SELECT 1 FROM candidate_profiles
                                    WHERE cpf = :cpf
                        )AS existe
            """,
            nativeQuery = true
    )
    boolean existsByCpf(@Param("cpf") String cpf);

    Optional<CandidateProfile> findByCpf(String cpf);
}
