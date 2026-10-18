package com.unisal.selectaapi.notificacoes.repository;

import com.unisal.selectaapi.notificacoes.entity.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    @Modifying
    @Query("""
            UPDATE Notificacao t
            SET t.lida = true,
                        t.lidaEm = :lida_em
            
            WHERE t.idNotificacao = :notificacao_id
                        AND t.user.id = :dono_da_consulta
                        AND t.lida = false
                
                """)
    int marcarMensagemComoLida(@Param("notificacao_id") UUID notificacaoId,
                               @Param("dono_da_consulta") UUID donoDaConsulta,
                               @Param("lida_em") Instant lidaEm);

    @Modifying
    @Query("""
        UPDATE Notificacao n
        SET n.lida = true, n.lidaEm = :date
        WHERE n.user.id = :user_id
                AND n.lida = false

        """)
    int marcarTodasMensagensComoLidasPeloUserId(@Param("user_id") UUID userId, @Param("date") Instant localDateTime);

    //Query para garantir que apenas os campos corretos sejam alterados, evitando recarregar a entidade inteira a cada save
    @Query("""
            SELECT n
            FROM Notificacao n
            WHERE n.lida = false AND n.user.id = :user_id
            ORDER BY n.createdAt DESC
        """)
    List<Notificacao> pegarNotificacoes(@Param("user_id") UUID userId, Pageable pageable);

    @Query("""
        SELECT COUNT(*) FROM Notificacao n
        WHERE n.lida = false 
                AND n.user.id = :user_id
        """)
    Long pegarQuantasNaoLidas(@Param("user_id") UUID userID);


}
