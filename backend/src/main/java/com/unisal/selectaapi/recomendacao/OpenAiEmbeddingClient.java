package com.unisal.selectaapi.recomendacao;

import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.unisal.selectaapi.recomendacao.config.EmbeddingProperties;

@Component
public class OpenAiEmbeddingClient {

    private final EmbeddingProperties properties;
    private final RestClient restClient;

    public OpenAiEmbeddingClient(EmbeddingProperties properties) {
        this.properties = properties;

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public float[] embed(String text) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("OPENAI_API_KEY não configurada");
        }

        var request = new EmbeddingRequest(properties.getModel(), text, EmbeddingProperties.DIMENSIONS, "float");

        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("Resposta vazia da API de embeddings da OpenAI");
        }

        float[] embedding = response.data().get(0).embedding();
        if (embedding == null || embedding.length != EmbeddingProperties.DIMENSIONS) {
            throw new IllegalStateException("Embedding retornado com dimensão inesperada");
        }
        return embedding;
    }

    record EmbeddingRequest(String model, String input, Integer dimensions, String encoding_format) {
    }

    record EmbeddingResponse(String object, List<EmbeddingData> data, String model, Usage usage) {
    }

    record EmbeddingData(String object, Integer index, float[] embedding) {
    }

    record Usage(Integer prompt_tokens, Integer total_tokens) {
    }
}
