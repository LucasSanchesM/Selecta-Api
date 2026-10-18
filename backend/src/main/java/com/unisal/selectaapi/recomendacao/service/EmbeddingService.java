package com.unisal.selectaapi.recomendacao.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.recomendacao.entity.TextEmbedding;
import com.unisal.selectaapi.recomendacao.entity.TipoEmbedding;
import com.unisal.selectaapi.recomendacao.repository.TextEmbeddingRepository;

/**
 * Devolve os vetores dos textos informados, chamando a API de embeddings
 * apenas para os textos novos ou alterados desde a última geração.
 */
@Service
public class EmbeddingService {

    private final TextEmbeddingRepository textEmbeddingRepository;
    private final EmbeddingClient embeddingClient;

    public EmbeddingService(TextEmbeddingRepository textEmbeddingRepository, EmbeddingClient embeddingClient) {
        this.textEmbeddingRepository = textEmbeddingRepository;
        this.embeddingClient = embeddingClient;
    }

    // Transação própria: a listagem de vagas roda em transação somente leitura, e uma falha
    // da API aqui não pode marcar a transação de quem chamou para rollback.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<UUID, float[]> obterVetores(TipoEmbedding tipo, Map<UUID, String> textosPorId) {
        if (textosPorId.isEmpty()) {
            return Map.of();
        }

        String modelo = embeddingClient.getModel();
        Map<UUID, TextEmbedding> salvos = textEmbeddingRepository.findAllById(textosPorId.keySet()).stream()
                .collect(Collectors.toMap(TextEmbedding::getSourceId, Function.identity()));

        Map<UUID, float[]> vetores = new HashMap<>();
        List<UUID> pendentesIds = new ArrayList<>();
        List<String> pendentesTextos = new ArrayList<>();
        List<String> pendentesHashes = new ArrayList<>();

        textosPorId.forEach((id, texto) -> {
            String hash = calcularHash(modelo, texto);
            TextEmbedding salvo = salvos.get(id);
            if (salvo != null && hash.equals(salvo.getContentHash())) {
                vetores.put(id, salvo.getVetor());
            } else {
                pendentesIds.add(id);
                pendentesTextos.add(texto);
                pendentesHashes.add(hash);
            }
        });

        if (!pendentesIds.isEmpty()) {
            List<float[]> novosVetores = embeddingClient.gerarEmbeddings(pendentesTextos);
            List<TextEmbedding> paraSalvar = new ArrayList<>(pendentesIds.size());
            for (int i = 0; i < pendentesIds.size(); i++) {
                UUID id = pendentesIds.get(i);
                TextEmbedding embedding = salvos.getOrDefault(id, new TextEmbedding(id, tipo));
                embedding.atualizar(pendentesHashes.get(i), modelo, novosVetores.get(i));
                paraSalvar.add(embedding);
                vetores.put(id, novosVetores.get(i));
            }
            textEmbeddingRepository.saveAll(paraSalvar);
        }

        return vetores;
    }

    static String calcularHash(String modelo, String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((modelo + "\n" + texto).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
