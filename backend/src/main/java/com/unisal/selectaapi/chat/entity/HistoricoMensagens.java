package com.unisal.selectaapi.chat.entity;

import com.unisal.selectaapi.candidatura.entity.Candidatura;
import com.unisal.selectaapi.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historico_mensagens")
public class HistoricoMensagens {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_mensagem")
    private UUID idMensagem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "mensagem", nullable = false, length = 300)
    private String mensagem;

    @Column(name = "lida", nullable = false)
    private Boolean lida;

    @Column(name = "lida_em")
    private Instant lidaEm;

    @Column(nullable = false, updatable = false)
    private Instant createdTimeStamp;

    public HistoricoMensagens(UUID idMensagem, Candidatura candidatura, User user, String mensagem) {
        this.idMensagem = idMensagem;
        this.candidatura = candidatura;
        this.user = user;
        this.mensagem = mensagem;
    }

    public Instant getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    public UUID getIdMensagem() {
        return idMensagem;
    }

    public void setIdMensagem(UUID idMensagem) {
        this.idMensagem = idMensagem;
    }

    public Candidatura getCandidatura() {
        return candidatura;
    }

    public void setCandidatura(Candidatura candidatura) {
        this.candidatura = candidatura;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMensagem() {
        return mensagem;
    }

    public Instant getLida_Em() {
        return lidaEm;
    }

    public void setLida_Em(Instant lida_Em) {
        this.lidaEm = lida_Em;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public Boolean getLida() {
        return lida;
    }

    public void setLida(Boolean lida) {
        this.lida = lida;
    }

    public HistoricoMensagens() {
    }

    @PrePersist
    private void onCreate(){
        this.createdTimeStamp = Instant.now();
        this.lida = false;
        this.lidaEm = null;
    }



}
