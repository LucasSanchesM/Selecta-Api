package com.unisal.selectaapi.vagas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.unisal.selectaapi.vagas.entity.ModalidadeVaga;
import com.unisal.selectaapi.vagas.entity.Vaga;

import jakarta.persistence.criteria.Predicate;

public class VagaSpecification {

    private VagaSpecification() {
    }

    public static Specification<Vaga> withFilters(
            String titulo,
            VagaRole categoria,
            ModalidadeVaga modalidade,
            BigDecimal salarioMin,
            BigDecimal salarioMax,
            String cidade,
            String estado,
            Boolean ativa,
            UUID companyProfileId
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (titulo != null && !titulo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("titulo")), "%" + titulo.toLowerCase() + "%"));
            }

            if (categoria != null) {
                predicates.add(cb.equal(root.get("categoria"), categoria));
            }

            if (modalidade != null) {
                predicates.add(cb.equal(root.get("modalidade"), modalidade));
            }

            if (salarioMin != null || salarioMax != null) {
                predicates.add(cb.greaterThan(root.get("salario"), BigDecimal.ZERO));
            }

            if (salarioMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salario"), salarioMin));
            }

            if (salarioMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salario"), salarioMax));
            }

            if (cidade != null && !cidade.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("cidade")), "%" + cidade.toLowerCase() + "%"));
            }

            if (estado != null && !estado.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("estado")), estado.toLowerCase()));
            }

            if (ativa != null) {
                predicates.add(cb.equal(root.get("ativa"), ativa));
            }

            if (companyProfileId != null) {
                predicates.add(cb.equal(root.get("empresa").get("id"), companyProfileId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
