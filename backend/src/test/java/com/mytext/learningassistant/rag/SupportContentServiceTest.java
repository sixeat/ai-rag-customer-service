package com.mytext.learningassistant.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.mytext.learningassistant.material.LearningMaterialEntity;
import com.mytext.learningassistant.material.LearningMaterialRepository;
import com.mytext.learningassistant.material.MaterialService;
import com.mytext.learningassistant.material.MaterialTextStatus;

import org.junit.jupiter.api.Test;

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
