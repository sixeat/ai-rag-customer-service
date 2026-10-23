package com.mytext.learningassistant.vector;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.mytext.learningassistant.material.LearningMaterialEntity;
import com.mytext.learningassistant.material.MaterialChunkEntity;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;

/** Spring AI Qdrant store backed by the vectors already persisted with each chunk. */
public class SpringAiQdrantVectorStoreClient implements VectorStoreClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiQdrantVectorStoreClient.class);
    private static final String QUERY = "__precomputed_query__";

    private final VectorStoreProperties properties;
    private final PrecomputedEmbeddingModel embeddingModel;
    private final QdrantVectorStore store;
    private final QdrantClient nativeClient;
    private volatile boolean schemaReady;

    public SpringAiQdrantVectorStoreClient(VectorStoreProperties properties, int grpcPort) {
        this.properties = properties;
        this.embeddingModel = new PrecomputedEmbeddingModel();
        if (properties.qdrantConfigured()) {
            URI uri = URI.create(properties.baseUrl());
            QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                uri.getHost(), grpcPort, "https".equalsIgnoreCase(uri.getScheme()), false)
                .withTimeout(properties.timeout());
            if (!properties.apiKey().isBlank()) {
                builder.withApiKey(properties.apiKey());
            }
            this.nativeClient = new QdrantClient(builder.build());
            this.store = QdrantVectorStore.builder(nativeClient, embeddingModel)
                .collectionName(properties.collection())
                .initializeSchema(true)
                .build();
        } else {
            this.nativeClient = null;
            this.store = null;
        }
    }

    SpringAiQdrantVectorStoreClient(VectorStoreProperties properties, QdrantVectorStore store) {
        this.properties = properties;
        this.embeddingModel = new PrecomputedEmbeddingModel();
        this.store = store;
        this.nativeClient = null;
    }

    @PreDestroy
    public void close() {
        if (nativeClient != null) {
            nativeClient.close();
        }
    }

    @Override
    public boolean configured() {
        return store != null;
    }

    @Override
    public void upsertChunks(long userId, LearningMaterialEntity material, List<MaterialChunkEntity> chunks,
                             Map<Long, List<Double>> embeddingsByChunkId) {
        if (!configured() || material == null || chunks == null || embeddingsByChunkId == null) {
            return;
        }
        List<Document> documents = new ArrayList<>();
        Map<String, float[]> vectors = new HashMap<>();
        for (MaterialChunkEntity chunk : chunks) {
            if (chunk.getId() == null || material.getId() == null) {
                continue;
            }
            List<Double> vector = embeddingsByChunkId.get(chunk.getId());
            if (vector == null || vector.isEmpty()) {
                continue;
            }
            String id = pointId(userId, material.getId(), chunk.getId()).toString();
            documents.add(Document.builder().id(id).text(chunk.getChunkText()).metadata(Map.of(
                "userId", userId,
                "materialId", material.getId(),
                "chunkId", chunk.getId(),
                "chunkIndex", chunk.getChunkIndex() == null ? -1 : chunk.getChunkIndex(),
                "title", material.getTitle() == null ? "" : material.getTitle(),
                "summary", chunk.getSummary() == null ? "" : chunk.getSummary(),
                "keywords", chunk.getKeywords() == null ? "" : chunk.getKeywords()
            )).build());
            vectors.put(id, toFloatArray(vector));
        }
        if (documents.isEmpty()) {
            return;
        }
        embeddingModel.useDocuments(vectors);
        try {
            ensureSchema();
            store.add(documents);
        } catch (Exception exception) {
            log.warn("Spring AI Qdrant upsert failed: collection={}, materialId={}",
                properties.collection(), material.getId(), exception);
        } finally {
            embeddingModel.clear();
        }
    }

    @Override
    public void deleteMaterial(long userId, long materialId) {
        if (!configured()) {
            return;
        }
        try {
            store.delete(filter(userId, materialId));
        } catch (Exception exception) {
            log.warn("Spring AI Qdrant delete failed: collection={}, materialId={}",
                properties.collection(), materialId, exception);
        }
    }

    @Override
    public List<VectorSearchResult> search(long userId, Long materialId, List<Double> queryEmbedding,
                                           int limit, double scoreThreshold) {
        if (!configured() || queryEmbedding == null || queryEmbedding.isEmpty() || limit <= 0) {
            return List.of();
        }
        embeddingModel.useQuery(toFloatArray(queryEmbedding));
        try {
            ensureSchema();
            SearchRequest request = SearchRequest.builder()
                .query(QUERY)
                .topK(limit)
                .similarityThreshold(Math.max(0.0, Math.min(1.0, scoreThreshold)))
                .filterExpression(filter(userId, materialId))
                .build();
            List<VectorSearchResult> results = new ArrayList<>();
            for (Document document : store.similaritySearch(request)) {
                long owner = number(document.getMetadata().get("userId"));
                long resultMaterialId = number(document.getMetadata().get("materialId"));
                long chunkId = number(document.getMetadata().get("chunkId"));
                if (owner == userId && (materialId == null || resultMaterialId == materialId)
                    && resultMaterialId > 0 && chunkId > 0 && document.getScore() != null) {
                    results.add(new VectorSearchResult(resultMaterialId, chunkId, document.getScore()));
                }
            }
            return results;
        } catch (Exception exception) {
            log.warn("Spring AI Qdrant search failed: collection={}, userId={}",
                properties.collection(), userId, exception);
            return List.of();
        } finally {
            embeddingModel.clear();
        }
    }

    private org.springframework.ai.vectorstore.filter.Filter.Expression filter(long userId, Long materialId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var owner = builder.eq("userId", userId);
        return materialId == null ? owner.build()
            : builder.and(owner, builder.eq("materialId", materialId)).build();
    }

    private void ensureSchema() throws Exception {
        if (!schemaReady) {
            synchronized (this) {
                if (!schemaReady) {
                    store.afterPropertiesSet();
                    schemaReady = true;
                }
            }
        }
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : -1;
    }

    private static float[] toFloatArray(List<Double> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }

    private static UUID pointId(long userId, long materialId, long chunkId) {
        return UUID.nameUUIDFromBytes(("u:" + userId + ":m:" + materialId + ":c:" + chunkId)
            .getBytes(StandardCharsets.UTF_8));
    }

    private static final class PrecomputedEmbeddingModel implements EmbeddingModel {
        private final ThreadLocal<Map<String, float[]>> documentVectors = new ThreadLocal<>();
        private final ThreadLocal<float[]> queryVector = new ThreadLocal<>();
        private final AtomicInteger dimensions = new AtomicInteger();

        void useDocuments(Map<String, float[]> vectors) {
            documentVectors.set(vectors);
            dimensions.set(vectors.values().iterator().next().length);
        }

        void useQuery(float[] vector) {
            queryVector.set(vector);
            dimensions.set(vector.length);
        }

        void clear() {
            documentVectors.remove();
            queryVector.remove();
        }

        @Override
        public float[] embed(String text) {
            float[] vector = queryVector.get();
            if (!QUERY.equals(text) || vector == null) {
                throw new IllegalStateException("No query embedding was supplied");
            }
            return vector;
        }

        @Override
        public float[] embed(Document document) {
            Map<String, float[]> vectors = documentVectors.get();
            float[] vector = vectors == null ? null : vectors.get(document.getId());
            if (vector == null) {
                throw new IllegalStateException("No document embedding was supplied");
            }
            return vector;
        }

        @Override
        public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy strategy) {
            return documents.stream().map(this::embed).toList();
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            throw new UnsupportedOperationException("Embeddings are supplied by the material pipeline");
        }

        @Override
        public int dimensions() {
            return dimensions.get();
        }
    }
}
