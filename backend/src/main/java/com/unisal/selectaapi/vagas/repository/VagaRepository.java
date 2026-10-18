package com.unisal.selectaapi.vagas.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unisal.selectaapi.vagas.entity.Vaga;

public interface VagaRepository extends JpaRepository<Vaga, UUID>, JpaSpecificationExecutor<Vaga> {

    @EntityGraph(attributePaths = "etapas")
    List<Vaga> findByEmpresaId(UUID companyProfileId);

    @EntityGraph(attributePaths = {"etapas", "empresa"})
    List<Vaga> findByAtivaTrue();


    //consulta banco com filtro de raio
    @Query(value = """
            SELECT *
            FROM jobs
            WHERE ativa = true
            AND (
                6371 * acos(
                    cos(radians(:lat))
                    * cos(radians(latitude))
                    * cos(radians(longitude) - radians(:lng))
                    + sin(radians(:lat))
                    * sin(radians(latitude))
                )
            ) <= :raio
            """, nativeQuery = true)
    List<Vaga> listarVagasPorRaio(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("raio") Double raioKm
    );
}
