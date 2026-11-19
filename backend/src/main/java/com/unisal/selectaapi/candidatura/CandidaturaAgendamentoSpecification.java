package com.unisal.selectaapi.candidatura;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.unisal.selectaapi.candidatura.entity.CandidaturaAgendamento;
import com.unisal.selectaapi.candidatura.entity.StatusAgendamento;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Filtros da listagem geral de agendamentos da empresa.
 * Mesmo padrão do {@link CandidaturaSpecification}: filtros opcionais montados
 * dinamicamente, sempre restritos às vagas da empresa informada.
 */
public class CandidaturaAgendamentoSpecification {

    private CandidaturaAgendamentoSpecification() {
    }

    public static Specification<CandidaturaAgendamento> daEmpresa(
            UUID empresaId,
            UUID criadoPorUserId,
            StatusAgendamento status,
            Instant de,
            Instant ate) {

        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            // A empresa logada só enxerga agendamentos de candidaturas das próprias vagas
            predicates.add(builder.equal(
                    root.get("candidatura").get("vaga").get("empresa").get("id"), empresaId));

            if (criadoPorUserId != null) {
                predicates.add(builder.equal(root.get("criadoPorUserId"), criadoPorUserId));
            }

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            // Janela de datas: [de, ate)
            if (de != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("inicio"), de));
            }
            if (ate != null) {
                predicates.add(builder.lessThan(root.get("inicio"), ate));
            }

            if (query != null && !isCountQuery(query)) {
                // O mapper lê candidato, vaga e etapa de cada linha; busca tudo numa única consulta
                Fetch<?, ?> candidatura = root.fetch("candidatura", JoinType.INNER);
                candidatura.fetch("vaga", JoinType.INNER);
                candidatura.fetch("candidato", JoinType.INNER).fetch("user", JoinType.INNER);
                root.fetch("etapa", JoinType.INNER);

                query.orderBy(builder.asc(root.get("inicio")));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> type = query.getResultType();
        return Long.class.equals(type) || long.class.equals(type);
    }
}
