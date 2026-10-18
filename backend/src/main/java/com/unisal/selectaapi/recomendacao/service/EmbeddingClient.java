package com.unisal.selectaapi.recomendacao.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.unisal.selectaapi.exception.BusinessException;

/**
 * Cliente do endpoint {@code POST /embeddings} no formato da API da OpenAI.
 * O mesmo formato é aceito pelo Ollama ({@code http://localhost:11434/v1}),
 * então trocar de provedor é só mudar a configuração.
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);
    private static final int TAMANHO_LOTE = 64;

    private final RestClient restClient;
    private final String model;

    public EmbeddingClient(
            @Value("${recomendacao.embedding.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${recomendacao.embedding.api-key:}") String apiKey,
            @Value("${recomendacao.embedding.model:text-embedding-3-small}") String model) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(removeTrailingSlash(baseUrl.trim()))
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (!apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        }

        this.restClient = builder.build();
        this.model = model.trim();
    }

    public String getModel() {
        return model;
    }

    /** Gera um vetor por texto, na mesma ordem da lista recebida. */
    public List<float[]> gerarEmbeddings(List<String> textos) {
        List<float[]> vetores = new ArrayList<>(textos.size());
        for (int inicio = 0; inicio < textos.size(); inicio += TAMANHO_LOTE) {
            List<String> lote = textos.subList(inicio, Math.min(inicio + TAMANHO_LOTE, textos.size()));
            vetores.addAll(chamarApi(lote));
        }
        return vetores;
    }

    private List<float[]> chamarApi(List<String> lote) {
        EmbeddingApiResponse resposta;
        try {
            resposta = restClient.post()
                    .uri("/embeddings")
                    .body(new EmbeddingApiRequest(model, lote))
                    .retrieve()
                    .body(EmbeddingApiResponse.class);
        } catch (RestClientException exception) {
            log.warn("Falha ao gerar embeddings com o modelo {}: {}", model, exception.getMessage());
            throw servicoIndisponivel();
        }

        if (resposta == null || resposta.data() == null || resposta.data().size() != lote.size()
                || resposta.data().stream().anyMatch(item -> item.embedding() == null)) {
            log.warn("Resposta inválida da API de embeddings para {} textos", lote.size());
            throw servicoIndisponivel();
        }

        return resposta.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingApiResponse.Item::index))
                .map(EmbeddingApiResponse.Item::embedding)
                .toList();
    }

    private BusinessException servicoIndisponivel() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                "O serviço de recomendação está indisponível no momento. Tente novamente mais tarde");
    }

    private static String removeTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    record EmbeddingApiRequest(String model, List<String> input) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingApiResponse(List<Item> data) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Item(int index, float[] embedding) {
        }
    }
}
