package com.unisal.selectaapi.candidatura.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.unisal.selectaapi.candidatura.entity.CandidaturaAgendamento;

@Repository
public interface CandidaturaAgendamentoRepository extends JpaRepository<CandidaturaAgendamento, UUID>, JpaSpecificationExecutor<CandidaturaAgendamento> {

    List<CandidaturaAgendamento> findByCandidaturaIdOrderByInicioAsc(UUID candidaturaId);

    Optional<CandidaturaAgendamento> findByIdAndCandidaturaId(UUID id, UUID candidaturaId);
}