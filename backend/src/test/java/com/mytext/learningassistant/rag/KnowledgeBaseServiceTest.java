package com.mytext.learningassistant.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.mytext.learningassistant.common.BusinessException;
import com.mytext.learningassistant.embedding.EmbeddingClient;
import com.mytext.learningassistant.llm.LlmCompletion;
import com.mytext.learningassistant.llm.ThirdPartyLlmClient;
import com.mytext.learningassistant.material.LearningMaterialEntity;
import com.mytext.learningassistant.material.LearningMaterialRepository;
import com.mytext.learningassistant.material.MaterialChunkEntity;
import com.mytext.learningassistant.material.MaterialChunkRepository;
import com.mytext.learningassistant.material.MaterialTextStatus;
import com.mytext.learningassistant.vector.VectorStoreClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KnowledgeBaseServiceTest {
    private final KnowledgeBaseRepository bases = mock(KnowledgeBaseRepository.class);
    private final KnowledgeBaseMaterialRepository memberships = mock(KnowledgeBaseMaterialRepository.class);
    private final LearningMaterialRepository materials = mock(LearningMaterialRepository.class);
    private final MaterialChunkRepository chunks = mock(MaterialChunkRepository.class);
    private final EmbeddingClient embeddings = mock(EmbeddingClient.class);
    private final VectorStoreClient vectors = mock(VectorStoreClient.class);
    private final ThirdPartyLlmClient llm = mock(ThirdPartyLlmClient.class);
    private final KnowledgeBaseService service = new KnowledgeBaseService(
        bases, memberships, materials, chunks, embeddings, vectors, llm);

    @BeforeEach
    void ownedBase() {
        KnowledgeBaseEntity base = new KnowledgeBaseEntity();
        base.setOwnerId(7L);
        when(bases.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(base));
    }

    @Test
    void rejectsForeignMaterialBeforeAddingMembership() {
        when(materials.findByIdAndOwnerId(20L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addMaterial(7L, 1L, 20L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("资料不存在");
        verify(memberships, never()).save(any());
    }

    @Test
    void asksOnlyUsingOwnedLinkedMaterials() {
        KnowledgeBaseMaterialEntity ownLink = new KnowledgeBaseMaterialEntity();
        ownLink.setMaterialId(10L);
        KnowledgeBaseMaterialEntity foreignLink = new KnowledgeBaseMaterialEntity();
        foreignLink.setMaterialId(20L);
        when(memberships.findByKnowledgeBaseId(1L)).thenReturn(List.of(ownLink, foreignLink));

        LearningMaterialEntity own = new LearningMaterialEntity();
        own.setId(10L);
        own.setTitle("退换货政策");
        own.setTextStatus(MaterialTextStatus.READY);
        when(materials.findByIdAndOwnerId(10L, 7L)).thenReturn(Optional.of(own));
        when(materials.findByIdAndOwnerId(20L, 7L)).thenReturn(Optional.empty());

        MaterialChunkEntity chunk = new MaterialChunkEntity();
        chunk.setId(100L);
        chunk.setMaterialId(10L);
        chunk.setChunkText("退货期限为七天，商品应保持完好。");
        when(chunks.findByMaterialIdOrderByChunkIndexAsc(10L)).thenReturn(List.of(chunk));
        when(llm.answerWithSystemPrompt(eq(7L), any(), any(), any()))
            .thenReturn(Optional.of(new LlmCompletion("七天内可退货。", "test-model")));

        KnowledgeBaseAnswer answer = service.ask(7L, 1L, new KnowledgeBaseAskRequest("退货期限是多久"));

        assertThat(answer.answer()).isEqualTo("七天内可退货。");
        assertThat(answer.sources()).extracting(RagSourceResponse::materialId).containsExactly(10L);
        verify(chunks, never()).findByMaterialIdOrderByChunkIndexAsc(20L);
    }
}
