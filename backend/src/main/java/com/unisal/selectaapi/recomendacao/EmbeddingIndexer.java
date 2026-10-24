package com.unisal.selectaapi.recomendacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.unisal.selectaapi.candidate.repository.CandidateProfileRepository;
import com.unisal.selectaapi.recomendacao.config.EmbeddingProperties;
import com.unisal.selectaapi.vagas.repository.VagaRepository;

@Service
public class EmbeddingIndexer {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexer.class);

    private final EmbeddingProperties properties;
    private final EmbeddingSchemaInitializer schema;
    private final EmbeddingRepository embeddingRepository;
    private final EmbeddingTextBuilder textBuilder;
    private final OpenAiEmbeddingClient openAiClient;
    private final CandidateProfileRepository candidateProfileRepository;
    private final VagaRepository vagaRepository;
    private final TransactionTemplate readOnlyTx;

    public EmbeddingIndexer(EmbeddingProperties properties,
            EmbeddingSchemaInitializer schema,
            EmbeddingRepository embeddingRepository,
            EmbeddingTextBuilder textBuilder,
            OpenAiEmbeddingClient openAiClient,
            CandidateProfileRepository candidateProfileRepository,
            VagaRepository vagaRepository,
            PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.schema = schema;
        this.embeddingRepository = embeddingRepository;
        this.textBuilder = textBuilder;
        this.openAiClient = openAiClient;
        this.candidateProfileRepository = candidateProfileRepository;
        this.vagaRepository = vagaRepository;
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
    }

    public void process(EmbeddingJobMessage message) {
        if (!properties.isEnabled() || !schema.isAvailable() || message == null || message.id() == null) {
            return;
        }

        Optional<Source> source = readOnlyTx.execute(status -> loadSource(message.target(), message.id()));

        if (source == null || source.isEmpty()) {
            embeddingRepository.delete(message.target(), message.id());
            return;
        }

        String text = source.get().text();
        String model = properties.getModel();
        String hash = sha256(model + "\n" + text);

        Optional<String> currentHash = embeddingRepository.findContentHash(message.target(), message.id());
        if (currentHash.isPresent() && currentHash.get().equals(hash)) {
            embeddingRepository.touch(message.target(), message.id(), source.get().updatedAt());
            return;
        }

        float[] embedding = openAiClient.embed(text);
        embeddingRepository.upsert(message.target(), message.id(), embedding, model, hash, source.get().updatedAt());
        log.info("Embedding atualizado: {} {}", message.target(), message.id());
    }

    private Optional<Source> loadSource(EmbeddingTarget target, UUID id) {
        if (target == EmbeddingTarget.CANDIDATO) {
            return candidateProfileRepository.findById(id)
                    .map(profile -> new Source(textBuilder.buildCandidato(profile), profile.getUpdatedAt()))
                    .filter(Source::hasText);
        }
        return vagaRepository.findById(id)
                .map(vaga -> new Source(textBuilder.buildVaga(vaga), vaga.getUpdatedAt()))
                .filter(Source::hasText);
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record Source(String text, Instant updatedAt) {
        boolean hasText() {
            return text != null && !text.isBlank();
        }
    }
}
