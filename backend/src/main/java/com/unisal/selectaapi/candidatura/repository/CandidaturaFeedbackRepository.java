package com.unisal.selectaapi.candidatura.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.unisal.selectaapi.vagas.entity.TipoDeEtapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unisal.selectaapi.candidatura.entity.CandidaturaFeedback;
import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;

@Repository
public interface CandidaturaFeedbackRepository extends JpaRepository<CandidaturaFeedback, UUID> {

    List<CandidaturaFeedback> findByCandidaturaIdOrderByCreatedAtAsc(UUID candidaturaId);

    List<CandidaturaFeedback> findByCandidaturaIdAndEtapaOrderByCreatedAtAsc(UUID candidaturaId, TipoDeEtapa etapa);

    Optional<CandidaturaFeedback> findByIdAndCandidaturaId(UUID id, UUID candidaturaId);
}
