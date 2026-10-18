package com.unisal.selectaapi.recomendacao.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.unisal.selectaapi.recomendacao.entity.TipoEmbedding;
import com.unisal.selectaapi.recomendacao.repository.TextEmbeddingRepository;
import com.unisal.selectaapi.vagas.event.VagaRemovidaEvent;
import com.unisal.selectaapi.vagas.event.VagaSalvaEvent;
import com.unisal.selectaapi.vagas.repository.VagaRepository;

/**
 * Gera o embedding da vaga em segundo plano, depois que o cadastro ou a edição
 * já foi gravado. Se falhar, a listagem gera o vetor na próxima vez que precisar dele.
 */
@Component
public class VagaEmbeddingListener {

    private static final Logger log = LoggerFactory.getLogger(VagaEmbeddingListener.class);

    private final VagaRepository vagaRepository;
    private final EmbeddingService embeddingService;
    private final TextEmbeddingRepository textEmbeddingRepository;

    public VagaEmbeddingListener(VagaRepository vagaRepository, EmbeddingService embeddingService,
            TextEmbeddingRepository textEmbeddingRepository) {
        this.vagaRepository = vagaRepository;
        this.embeddingService = embeddingService;
        this.textEmbeddingRepository = textEmbeddingRepository;
    }

    @Async
    @TransactionalEventListener
    public void aoSalvarVaga(VagaSalvaEvent evento) {
        try {
            vagaRepository.findById(evento.vagaId())
                    .filter(vaga -> Boolean.TRUE.equals(vaga.getAtiva()))
                    .ifPresent(vaga -> {
                        String texto = TextoEmbedding.daVaga(vaga);
                        if (!texto.isBlank()) {
                            embeddingService.obterVetores(TipoEmbedding.VAGA, Map.of(vaga.getId(), texto));
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn("Não foi possível gerar o embedding da vaga {}: {}", evento.vagaId(), exception.getMessage());
        }
    }

    @Async
    @TransactionalEventListener
    public void aoRemoverVaga(VagaRemovidaEvent evento) {
        try {
            textEmbeddingRepository.deleteById(evento.vagaId());
        } catch (RuntimeException exception) {
            log.warn("Não foi possível remover o embedding da vaga {}: {}", evento.vagaId(), exception.getMessage());
        }
    }
}
