package com.unisal.selectaapi.vagas.repository;

import com.unisal.selectaapi.vagas.entity.TipoDeEtapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipoDeEtapaRepository extends JpaRepository<TipoDeEtapa, UUID> {

    List<TipoDeEtapa> findAllByCompanyProfile_Id(UUID companyProfileId);

    @Query("""
        SELECT t FROM TipoDeEtapa t
            WHERE t.companyProfile IS NULL
    """)
    List<TipoDeEtapa> findAllSystemEtapas();

    boolean existsByCompanyProfileIdAndNomeIgnoreCase(UUID companyProfileId, String nome);

    @Query("""
        SELECT t FROM TipoDeEtapa t
            WHERE (t.companyProfile.id = :company_profile_id
                OR t.companyProfile.id IS NULL) AND t.disponibilidade = true
    """)
    List<TipoDeEtapa> findEtapasDisponiveisParaEmpresa(@Param("company_profile_id") UUID companyProfileId);

    Optional<TipoDeEtapa> findByCodigoAndCompanyProfileIsNull(String codigo);

    @Query("""
        SELECT t FROM TipoDeEtapa t
        WHERE t.id = :etapaId
          AND (t.companyProfile.id = :companyProfileId
               OR t.companyProfile IS NULL)
    """)
    Optional<TipoDeEtapa> findByIdDisponivelParaEmpresa(
            @Param("etapaId") UUID etapaId,
            @Param("companyProfileId") UUID companyProfileId
    );}
