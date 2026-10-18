package com.unisal.selectaapi.notificacoes.service;

import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.notificacoes.listener.CriarNotificacaoEvent;
import com.unisal.selectaapi.notificacoes.dtos.NotificacaoResponseDto;
import com.unisal.selectaapi.notificacoes.dtos.PublishNotificacaoDto;
import com.unisal.selectaapi.notificacoes.entity.Notificacao;
import com.unisal.selectaapi.notificacoes.repository.NotificacaoRepository;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.entity.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NotificacaoService(NotificacaoRepository notificacaoRepository, UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.notificacaoRepository = notificacaoRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    private User procurarUsuario(UUID userId){
        return userRepository.findById(userId).
                                orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
    }

    @Transactional
    public NotificacaoResponseDto publicarNotificacao(UUID userId, PublishNotificacaoDto dto){
        var user = procurarUsuario(userId);
        var notificacao = new Notificacao();
        notificacao.setTitulo(dto.titulo());
        notificacao.setDescricao(dto.descricao());
        notificacao.setType(dto.type());
        notificacao.setUser(user);
        notificacao.setRecursoId(dto.recursoId());

        var saved = notificacaoRepository.save(notificacao);
        var response = new NotificacaoResponseDto(saved.getIdNotificacao(),
                                                    saved.getUser().getId(),
                                                    saved.getTitulo(),
                                                    saved.getDescricao(),
                                                    saved.getCreatedAt(),
                                                    saved.getType(),
                                                    saved.getRecursoId());


        eventPublisher.publishEvent(new CriarNotificacaoEvent(response, userId));
        return response;
    }

    @Transactional
    public void marcarNotificacaoComoLidaPeloId(UUID notificacaoId, UUID userId){
        var linhasAfetadas = notificacaoRepository.marcarMensagemComoLida(notificacaoId, userId, Instant.now());
        if(linhasAfetadas == 0){
            throw new BusinessException(HttpStatus.NOT_FOUND, "Notificacao listada como nao lida, nao foi encontrada");
        }
    }


    @Transactional
    public void marcarTodasNotificacoesComoLidas(UUID userId){
            notificacaoRepository.marcarTodasMensagensComoLidasPeloUserId(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponseDto> pegarNotificacoesNaoLidas(UUID userId, Pageable pageable){
        var lista = notificacaoRepository.pegarNotificacoes(userId, pageable);

        return lista.stream().
                        map(notificacao -> new NotificacaoResponseDto(
                                                    notificacao.getIdNotificacao(),
                                                    notificacao.getUser().getId(),
                                                    notificacao.getTitulo(),
                                                    notificacao.getDescricao(),
                                                    notificacao.getCreatedAt(),
                                                    notificacao.getType(),
                                                    notificacao.getRecursoId())).toList();
    }

    @Transactional(readOnly = true)
    public Long pegarQuantidadeDeNotificacoesNaoLidas(UUID userId){
        return notificacaoRepository.pegarQuantasNaoLidas(userId);
    }
}
