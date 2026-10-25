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
import java.util.Map;
import java.time.LocalDateTime;

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
import org.springframework.test.util.ReflectionTestUtils;

class KnowledgeBaseServiceTest {
    private final KnowledgeBaseRepository bases = mock(KnowledgeBaseRepository.class);
    private final KnowledgeBaseMaterialRepository memberships = mock(KnowledgeBaseMaterialRepository.class);
    private final LearningMaterialRepository materials = mock(LearningMaterialRepository.class);
    private final MaterialChunkRepository chunks = mock(MaterialChunkRepository.class);
    private final EmbeddingClient embeddings = mock(EmbeddingClient.class);
    private final VectorStoreClient vectors = mock(VectorStoreClient.class);
    private final SupportContentService supportContent = mock(SupportContentService.class);
    private final SupportModelRouter supportModels = mock(SupportModelRouter.class);
    private final ThirdPartyLlmClient llm = mock(ThirdPartyLlmClient.class);
    private final KnowledgeBaseService service = new KnowledgeBaseService(
        bases, memberships, materials, chunks, embeddings, vectors, llm, supportContent, supportModels);

    @BeforeEach
    void ownedBase() {
        KnowledgeBaseEntity base = new KnowledgeBaseEntity();
        ReflectionTestUtils.setField(base, "id", 1L);
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

    @Test
    void supportReadsOnlyPublishedAdministratorsMaterial() {
        KnowledgeBaseEntity published = new KnowledgeBaseEntity();
        ReflectionTestUtils.setField(published, "id", 2L);
        published.setOwnerId(99L);
        published.setScenario(KnowledgeBaseScenario.ECOMMERCE);
        published.setPublished(true);
        when(bases.findByScenarioAndPublishedTrue(KnowledgeBaseScenario.ECOMMERCE)).thenReturn(List.of(published));

        KnowledgeBaseMaterialEntity link = new KnowledgeBaseMaterialEntity();
        link.setMaterialId(10L);
        when(memberships.findByKnowledgeBaseId(2L)).thenReturn(List.of(link));

        LearningMaterialEntity material = new LearningMaterialEntity();
        material.setId(10L);
        material.setOwnerId(99L);
        material.setTitle("配送政策");
        material.setTextStatus(MaterialTextStatus.READY);
        when(materials.findByIdAndOwnerId(10L, 99L)).thenReturn(Optional.of(material));
        when(supportContent.eligibleBaseMaterials(eq(KnowledgeBaseScenario.ECOMMERCE), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        MaterialChunkEntity chunk = new MaterialChunkEntity();
        chunk.setId(100L);
        chunk.setMaterialId(10L);
        chunk.setChunkText("订单将在三个工作日内发货。");
        when(chunks.findByMaterialIdOrderByChunkIndexAsc(10L)).thenReturn(List.of(chunk));
        when(supportModels.answer(any(), any(), any(), any()))
            .thenReturn(Optional.of(new LlmCompletion("三个工作日内发货。", "test-model")));

        KnowledgeBaseAnswer answer = service.askScenario(7L, KnowledgeBaseScenario.ECOMMERCE, "订单何时发货");

        assertThat(answer.sources()).extracting(RagSourceResponse::materialId).containsExactly(10L);
        verify(materials, never()).findByIdAndOwnerId(10L, 7L);
        verify(supportModels).answer(any(), eq("订单何时发货"), any(), any());
    }

    @Test
    void expiredActivityReturnsEndTimeWithoutCallingModel() {
        SupportContentEntity expired = new SupportContentEntity();
        expired.setMaterialId(20L);
        expired.setTitle("周年庆双倍返利");
        expired.setEndsAt(LocalDateTime.of(2026, 10, 31, 23, 59));
        when(supportContent.matchingExpired(eq(KnowledgeBaseScenario.GAME), any(), any()))
            .thenReturn(Optional.of(expired));

        KnowledgeBaseAnswer answer = service.askScenario(7L, KnowledgeBaseScenario.GAME,
            "周年庆双倍返利现在还能参加吗");

        assertThat(answer.answer()).contains("2026-10-31 23:59", "已于");
        assertThat(answer.sources()).extracting(RagSourceResponse::materialId).containsExactly(20L);
        assertThat(answer.sources().getFirst().excerpt()).doesNotContain("返利规则");
        verify(supportModels, never()).answer(any(), any(), any(), any());
    }

    @Test
    void diagnosticUsesSameSourcesAndReportsModelRoute() {
        LearningMaterialEntity material = new LearningMaterialEntity();
        material.setId(10L);
        material.setOwnerId(99L);
        material.setTitle("周末优惠");
        material.setTextStatus(MaterialTextStatus.READY);
        when(supportContent.activeMaterials(KnowledgeBaseScenario.ECOMMERCE))
            .thenReturn(List.of(material));
        when(supportContent.diagnosticFilterCounts(KnowledgeBaseScenario.ECOMMERCE))
            .thenReturn(Map.of("ACTIVE", 1L, "EXPIRED", 1L));
        MaterialChunkEntity chunk = new MaterialChunkEntity();
        chunk.setId(100L);
        chunk.setMaterialId(10L);
        chunk.setChunkText("周末优惠于周日结束。");
        when(chunks.findByMaterialIdOrderByChunkIndexAsc(10L)).thenReturn(List.of(chunk));
        when(supportModels.answerDetailed(any(), any(), any(), any()))
            .thenReturn(Optional.of(new SupportModelRouteResult(
                new LlmCompletion("周日结束", "small-model", 12, 3, 15),
                SupportQuestionDifficultyClassifier.Difficulty.LOW, "SMALL", "SMALL", false)));

        SupportDiagnosticResponse result = service.diagnoseScenario(7L,
            KnowledgeBaseScenario.ECOMMERCE, "优惠什么时候结束？");

        assertThat(result.outcome()).isEqualTo("ANSWERED");
        assertThat(result.activeMaterials()).isEqualTo(1);
        assertThat(result.candidateChunks()).isEqualTo(1);
        assertThat(result.sources()).extracting(RagSourceResponse::materialId).containsExactly(10L);
        assertThat(result.modelName()).isEqualTo("small-model");
        assertThat(result.totalTokens()).isEqualTo(15);
        assertThat(result.contentFilters()).containsEntry("EXPIRED", 1L);
    }
}
