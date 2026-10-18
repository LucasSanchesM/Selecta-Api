package com.unisal.selectaapi.candidatura.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import com.unisal.selectaapi.candidatura.entity.Candidatura;
import com.unisal.selectaapi.candidatura.entity.StatusCandidatura;

@Repository
public interface CandidaturaRepository
        extends JpaRepository<Candidatura, UUID>, JpaSpecificationExecutor<Candidatura> {

    boolean existsByVagaIdAndCandidatoUserId(UUID vagaId, UUID userId);

    Optional<Candidatura> findByIdAndCandidatoUserId(UUID id, UUID userId);

    Optional<Candidatura> findByIdAndVagaEmpresaUserId(UUID id, UUID userId);

    Optional<Candidatura> findByIdAndVagaEmpresaId(UUID id, UUID empresaId);

    List<Candidatura> findByCandidatoUserIdOrderByCreatedAtDesc(UUID userId);

    List<Candidatura> findByVagaIdOrderByCreatedAtDesc(UUID vagaId);

    long countByVagaIdAndStatus(UUID vagaId, StatusCandidatura status);
}
