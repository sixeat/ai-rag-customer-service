package com.mytext.learningassistant.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mytext.learningassistant.material.LearningMaterialEntity;
import com.mytext.learningassistant.material.MaterialChunkEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;

class SpringAiQdrantVectorStoreClientTest {

    @Test
    void configuredClientCanInitializeWithoutAConnectedServer() {
        SpringAiQdrantVectorStoreClient client = new SpringAiQdrantVectorStoreClient(properties(), 6334);
        try {
            assertThat(client.configured()).isTrue();
        } finally {
            client.close();
        }
    }

    @Test
    void writesExistingVectorsAndFiltersSearchByOwnerAndMaterial() throws Exception {
        QdrantVectorStore store = mock(QdrantVectorStore.class);
        SpringAiQdrantVectorStoreClient client = new SpringAiQdrantVectorStoreClient(properties(), store);
        LearningMaterialEntity material = new LearningMaterialEntity();
        material.setId(11L);
        material.setOwnerId(7L);
        material.setTitle("Test material");
        MaterialChunkEntity chunk = new MaterialChunkEntity();
        chunk.setId(13L);
        chunk.setMaterialId(11L);
        chunk.setChunkIndex(0);
        chunk.setChunkText("TCP protocol");

        client.upsertChunks(7L, material, List.of(chunk), Map.of(13L, List.of(0.1, 0.2)));
        ArgumentCaptor<List<Document>> documents = ArgumentCaptor.forClass(List.class);
        verify(store).add(documents.capture());
        assertThat(documents.getValue()).hasSize(1);
        assertThat(documents.getValue().get(0).getId()).isEqualTo(
            UUID.nameUUIDFromBytes("u:7:m:11:c:13".getBytes(StandardCharsets.UTF_8)).toString());
        assertThat(documents.getValue().get(0).getMetadata()).containsEntry("userId", 7L)
            .containsEntry("materialId", 11L).containsEntry("chunkId", 13L);

        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
            Document.builder().id(documents.getValue().get(0).getId()).text("TCP protocol")
                .metadata(Map.of("userId", 7L, "materialId", 11L, "chunkId", 13L)).score(0.91).build(),
            Document.builder().id("other").text("private")
                .metadata(Map.of("userId", 8L, "materialId", 11L, "chunkId", 14L)).score(0.99).build()
        ));

        assertThat(client.search(7L, 11L, List.of(0.1, 0.2), 5, 0.55))
            .containsExactly(new VectorSearchResult(11L, 13L, 0.91));
        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(request.capture());
        assertThat(request.getValue().getFilterExpression().toString()).contains("userId", "materialId");
        verify(store).afterPropertiesSet();
    }

    private VectorStoreProperties properties() {
        return new VectorStoreProperties(true, "qdrant", "http://127.0.0.1:6333", "",
            "test_chunks", Duration.ofSeconds(2));
    }
}
