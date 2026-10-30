package com.unisal.selectaapi.candidatura;

import java.util.ArrayList;
import java.util.UUID;

import com.unisal.selectaapi.vagas.entity.TipoDeEtapa;
import org.springframework.data.jpa.domain.Specification;

import com.unisal.selectaapi.candidatura.entity.Candidatura;
import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.candidatura.entity.StatusCandidatura;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros do painel da empresa. Segue o mesmo padrão do VagaSpecification.
 */
public class CandidaturaSpecification {

    private CandidaturaSpecification() {
    }

    public static Specification<Candidatura> withFilters(
            UUID empresaId,
            UUID vagaId,
            StatusCandidatura status,
            TipoDeEtapa etapa,
            String candidatoNome) {

        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            // A empresa logada só pode ver candidaturas das próprias vagas
            predicates.add(builder.equal(
                    root.get("vaga").get("empresa").get("id"), empresaId));

            if (vagaId != null) {
                predicates.add(builder.equal(root.get("vaga").get("id"), vagaId));
            }

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (etapa != null) {
                predicates.add(builder.equal(root.get("etapaAtual"), etapa));
            }

            if (candidatoNome != null && !candidatoNome.isBlank()) {
                predicates.add(builder.like(
                        builder.lower(root.get("candidato").get("user").get("name")),
                        "%" + candidatoNome.toLowerCase() + "%"));
            }

            if (query != null) {
                query.orderBy(builder.desc(root.get("createdAt")));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
