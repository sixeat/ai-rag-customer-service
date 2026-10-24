package com.mytext.learningassistant.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.mytext.learningassistant.material.LearningMaterialEntity;
import com.mytext.learningassistant.material.LearningMaterialRepository;
import com.mytext.learningassistant.material.MaterialService;
import com.mytext.learningassistant.material.MaterialTextStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SupportContentServiceTest {
    private final SupportContentRepository contents = mock(SupportContentRepository.class);
    private final LearningMaterialRepository materials = mock(LearningMaterialRepository.class);
    private final SupportContentService service = new SupportContentService(contents, materials, mock(MaterialService.class));

    @Test
    void onlyPublishedEffectiveAndParsedContentIsRetrievable() {
        SupportContentEntity active = content(1L, SupportContentCategory.ECOMMERCE_PROMOTION, true);
        SupportContentEntity expired = content(2L, SupportContentCategory.ECOMMERCE_PROMOTION, true);
        expired.setEndsAt(LocalDateTime.now().minusMinutes(1));
        SupportContentEntity future = content(3L, SupportContentCategory.ECOMMERCE_PROMOTION, true);
        future.setStartsAt(LocalDateTime.now().plusMinutes(1));
        SupportContentEntity failed = content(4L, SupportContentCategory.ECOMMERCE_PROMOTION, true);
        SupportContentEntity withdrawn = content(5L, SupportContentCategory.ECOMMERCE_PROMOTION, false);
        when(contents.findByCategoryInAndPublishedTrue(List.of(SupportContentCategory.ECOMMERCE_PROMOTION)))
            .thenReturn(List.of(active, expired, future, failed));
        when(materials.findByIdAndOwnerId(1L, 9L)).thenReturn(Optional.of(material(1L, MaterialTextStatus.READY)));
        when(materials.findByIdAndOwnerId(4L, 9L)).thenReturn(Optional.of(material(4L, MaterialTextStatus.FAILED)));

        assertThat(service.activeMaterials(KnowledgeBaseScenario.ECOMMERCE))
            .extracting(LearningMaterialEntity::getId).containsExactly(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "周年庆双倍返利什么时候结束", "游戏周年庆双倍返利还有吗", "周年庆双倍返利活动规则是什么",
        "周年庆双倍返利现在能参加吗", "请问周年庆双倍返利截止时间", "周年庆双倍返利奖励如何领取",
        "周年庆双倍返利还有效吗", "周年庆双倍返利是昨天结束的吗", "周年庆双倍返利是否延期",
        "游戏里周年庆双倍返利怎么没了", "周年庆双倍返利活动几点关闭", "周年庆双倍返利能补领吗",
        "周年庆双倍返利是否正在进行", "周年庆双倍返利什么时候下线", "关于周年庆双倍返利的咨询"
    })
    void identifiesPublishedExpiredActivityBySpecificTitle(String question) {
        SupportContentEntity expired = content(2L, SupportContentCategory.GAME_EVENT, true);
        expired.setTitle("周年庆双倍返利");
        expired.setEndsAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusDays(1));
        when(contents.findByCategoryInAndPublishedTrue(List.of(
            SupportContentCategory.GAME_EVENT, SupportContentCategory.GAME_UPDATE)))
            .thenReturn(List.of(expired));
        when(materials.findByIdAndOwnerId(2L, 9L)).thenReturn(Optional.of(material(2L, MaterialTextStatus.READY)));

        assertThat(service.matchingExpired(KnowledgeBaseScenario.GAME, question, List.of()))
            .contains(expired);
    }

    @Test
    void expiredNoticeIgnoresDraftFutureAndSupersededContent() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        SupportContentEntity expired = content(2L, SupportContentCategory.GAME_EVENT, true);
        expired.setTitle("周年庆双倍返利");
        expired.setEndsAt(now.minusDays(1));
        SupportContentEntity future = content(3L, SupportContentCategory.GAME_EVENT, true);
        future.setTitle("未来周年庆双倍返利");
        future.setEndsAt(now.plusDays(1));
        when(contents.findByCategoryInAndPublishedTrue(List.of(
            SupportContentCategory.GAME_EVENT, SupportContentCategory.GAME_UPDATE)))
            .thenReturn(List.of(expired, future));
        when(materials.findByIdAndOwnerId(2L, 9L)).thenReturn(Optional.of(material(2L, MaterialTextStatus.READY)));

        assertThat(service.matchingExpired(KnowledgeBaseScenario.GAME, "其他游戏活动何时结束", List.of()))
            .isEmpty();
        LearningMaterialEntity current = material(4L, MaterialTextStatus.READY);
        current.setTitle("周年庆双倍返利");
        assertThat(service.matchingExpired(KnowledgeBaseScenario.GAME, "周年庆双倍返利何时结束", List.of(current)))
            .isEmpty();
        assertThat(service.matchingExpired(KnowledgeBaseScenario.GAME, "未来周年庆双倍返利何时结束", List.of()))
            .isEmpty();
    }

    @Test
    void publishedBaseCannotReintroduceExpiredOrWithdrawnSupportContent() {
        SupportContentEntity expired = content(2L, SupportContentCategory.GAME_EVENT, true);
        expired.setEndsAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusDays(1));
        SupportContentEntity withdrawn = content(3L, SupportContentCategory.GAME_EVENT, false);
        when(contents.findByMaterialIdIn(List.of(2L, 3L, 4L)))
            .thenReturn(List.of(expired, withdrawn));
        when(contents.findByCategoryInAndPublishedTrue(List.of(
            SupportContentCategory.GAME_EVENT, SupportContentCategory.GAME_UPDATE)))
            .thenReturn(List.of(expired));

        assertThat(service.eligibleBaseMaterials(KnowledgeBaseScenario.GAME, List.of(
            material(2L, MaterialTextStatus.READY), material(3L, MaterialTextStatus.READY),
            material(4L, MaterialTextStatus.READY))))
            .extracting(LearningMaterialEntity::getId).containsExactly(4L);
    }

    private SupportContentEntity content(long id, SupportContentCategory category, boolean published) {
        SupportContentEntity value = new SupportContentEntity();
        value.setMaterialId(id);
        value.setOwnerId(9L);
        value.setCategory(category);
        value.setPublished(published);
        return value;
    }

    private LearningMaterialEntity material(long id, MaterialTextStatus status) {
        LearningMaterialEntity value = new LearningMaterialEntity();
        value.setId(id);
        value.setTextStatus(status);
        return value;
    }
}
