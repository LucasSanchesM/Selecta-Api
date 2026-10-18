package com.unisal.selectaapi.chat.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unisal.selectaapi.candidatura.entity.Candidatura;
import com.unisal.selectaapi.chat.entity.HistoricoMensagens;

public interface HistoricoMensagensRepository
        extends JpaRepository<HistoricoMensagens, UUID> {

    List<HistoricoMensagens> findByCandidaturaOrderByCreatedTimeStampAsc(
            Candidatura candidatura);

    @Modifying
    @Query("""
            UPDATE HistoricoMensagens m
               SET m.lida = true,
                   m.lidaEm = :agora
             WHERE m.candidatura.id = :candidaturaId
               AND m.user.id <> :userId
               AND m.lida = false
            """)
    int marcarComoLidas(
            @Param("candidaturaId") UUID candidaturaId,
            @Param("userId") UUID userId,
            @Param("agora") Instant agora);

    @Query("""
            SELECT COUNT(m)
              FROM HistoricoMensagens m
             WHERE m.lida = false
               AND m.user.id = :user_id
               AND m.candidatura.id = :candidatura_id
            """)
    Long quantidadeDeMensagensNaoLidasPeloIdDaCandidatura(
            @Param("candidatura_id") UUID candidaturaId,
            @Param("user_id") UUID userId);

    /**
     * Lado da empresa: marca só o que o candidato enviou,
     * não as mensagens dos colegas.
     */
    @Modifying
    @Query("""
            UPDATE HistoricoMensagens m
               SET m.lida = true,
                   m.lidaEm = :agora
             WHERE m.candidatura.id = :candidaturaId
               AND m.user.id = :autorId
               AND m.lida = false
            """)
    int marcarComoLidasDoAutor(
            @Param("candidaturaId") UUID candidaturaId,
            @Param("autorId") UUID autorId,
            @Param("agora") Instant agora);
}