package com.unisal.selectaapi.recomendacao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingProperties {

    public static final int DIMENSIONS = 1536;

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int reconcileBatchSize;

    public EmbeddingProperties(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.embedding.model:text-embedding-3-small}") String model,
            @Value("${recomendacao.reconcile.batch-size:100}") int reconcileBatchSize) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.model = model;
        this.reconcileBatchSize = reconcileBatchSize;
    }

    public boolean isEnabled() {
        return !apiKey.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public int getReconcileBatchSize() {
        return reconcileBatchSize;
    }
}
