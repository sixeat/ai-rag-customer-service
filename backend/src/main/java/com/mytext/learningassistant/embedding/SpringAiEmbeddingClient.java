package com.mytext.learningassistant.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mytext.learningassistant.config.SpringAiOpenAiApiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/** Uses Spring AI for OpenAI-compatible embeddings while preserving Voyage input types. */
public class SpringAiEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiEmbeddingClient.class);

    private final EmbeddingProperties properties;
    private final EmbeddingModel model;
    private final EmbeddingClient voyageClient;

    public SpringAiEmbeddingClient(EmbeddingProperties properties) {
        this.properties = properties;
        if (properties.model().toLowerCase(Locale.ROOT).startsWith("voyage-multimodal-")) {
            // Spring AI 1.1.x has no Voyage multimodal model. Its document/query
            // input_type values are essential for compatibility with existing indexes.
            this.voyageClient = new VoyageMultimodalEmbeddingClient(properties);
            this.model = null;
        } else if (properties.configured()) {
            this.voyageClient = null;
            OpenAiApi api = SpringAiOpenAiApiFactory.create(
                properties.baseUrl(), properties.apiKey(), properties.timeout());
            this.model = new OpenAiEmbeddingModel(api, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(properties.model()).build());
        } else {
            this.voyageClient = null;
            this.model = null;
        }
    }

    @Override
    public Optional<List<Double>> embed(String text) {
        return embedDocument(text);
    }

    @Override
    public Optional<List<Double>> embedDocument(String text) {
        return embedValue(text, false);
    }

    @Override
    public Optional<List<Double>> embedQuery(String text) {
        return embedValue(text, true);
    }

    private Optional<List<Double>> embedValue(String text, boolean query) {
        if (!properties.configured() || text == null || text.isBlank()) {
            return Optional.empty();
        }
        if (voyageClient != null) {
            return query ? voyageClient.embedQuery(text) : voyageClient.embedDocument(text);
        }
        try {
            float[] embedding = model.embed(text);
            List<Double> values = new ArrayList<>(embedding.length);
            for (float value : embedding) {
                values.add((double) value);
            }
            return values.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(values));
        } catch (Exception exception) {
            log.warn("Spring AI embedding failed: model={}, message={}", properties.model(), exception.getMessage());
            return Optional.empty();
        }
    }
}
