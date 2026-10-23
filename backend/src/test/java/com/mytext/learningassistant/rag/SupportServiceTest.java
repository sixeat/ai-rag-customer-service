package com.mytext.learningassistant.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytext.learningassistant.common.BusinessException;
import com.mytext.learningassistant.llm.ThirdPartyLlmClient;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class SupportServiceTest {
    private final SupportConversationRepository conversations = mock(SupportConversationRepository.class);
    private final SupportMessageRepository messages = mock(SupportMessageRepository.class);
    private final KnowledgeBaseService knowledgeBases = mock(KnowledgeBaseService.class);
    private final ThirdPartyLlmClient llm = mock(ThirdPartyLlmClient.class);
    private final SupportService service = new SupportService(conversations, messages, knowledgeBases, llm, new ObjectMapper());

    @Test
    void routesOnlyUnambiguousScenario() {
        assertThat(SupportService.route("订单何时发货")).isEqualTo(KnowledgeBaseScenario.ECOMMERCE);
        assertThat(SupportService.route("游戏账号充值不到账")).isEqualTo(KnowledgeBaseScenario.GAME);
        assertThat(SupportService.route("你好")).isNull();
        assertThat(SupportService.requestsHuman("请转人工处理")).isTrue();
        assertThat(SupportService.requestsHuman("人工智能是什么")).isFalse();
        assertThat(SupportService.requiresHumanAction("我的订单在哪里")).isTrue();
        assertThat(SupportService.needsHumanAnswer("无法根据资料确认")).isTrue();
    }

    @Test
    void refusesAccessToAnotherUsersConversation() {
        when(conversations.findByIdAndOwnerId(5L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(5L, 7L, "订单问题"))
            .isInstanceOf(BusinessException.class).hasMessageContaining("不存在");
        verify(messages, never()).save(any());
    }

    @Test
    void directHumanRequestEntersQueueWithoutCallingKnowledgeBase() {
        SupportConversationEntity conversation = conversation();
        when(conversations.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(conversation));
        when(messages.findByConversationIdOrderByIdAsc(1L)).thenReturn(List.of());

        SupportConversationResponse response = service.send(7L, 1L, "我要转人工");

        assertThat(response.status()).isEqualTo(SupportStatus.WAITING_HUMAN);
        ArgumentCaptor<SupportMessageEntity> captor = ArgumentCaptor.forClass(SupportMessageEntity.class);
        verify(messages, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(SupportMessageEntity::getSender)
            .containsExactly(SupportSender.USER, SupportSender.AI);
        verify(knowledgeBases, never()).askScenario(anyLong(), any(), any());
    }

    @Test
    void missingEvidenceEntersHumanQueue() {
        SupportConversationEntity conversation = conversation();
        when(conversations.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(conversation));
        when(messages.findByConversationIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(knowledgeBases.askScenario(eq(7L), eq(KnowledgeBaseScenario.ECOMMERCE), any()))
            .thenReturn(new KnowledgeBaseAnswer("无依据", null, List.of()));

        SupportConversationResponse response = service.send(7L, 1L, "订单何时发货");

        assertThat(response.scenario()).isEqualTo(KnowledgeBaseScenario.ECOMMERCE);
        assertThat(response.status()).isEqualTo(SupportStatus.WAITING_HUMAN);
    }

    @Test
    void handoffButtonRecordsUserRequest() {
        SupportConversationEntity conversation = conversation();
        when(conversations.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(conversation));
        when(messages.findByConversationIdOrderByIdAsc(1L)).thenReturn(List.of());

        SupportConversationResponse response = service.requestHuman(7L, 1L);

        assertThat(response.status()).isEqualTo(SupportStatus.WAITING_HUMAN);
        ArgumentCaptor<SupportMessageEntity> captor = ArgumentCaptor.forClass(SupportMessageEntity.class);
        verify(messages, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getContent()).isEqualTo("请求转人工");
    }

    private SupportConversationEntity conversation() {
        SupportConversationEntity conversation = new SupportConversationEntity();
        ReflectionTestUtils.setField(conversation, "id", 1L);
        conversation.setOwnerId(7L);
        return conversation;
    }
}
