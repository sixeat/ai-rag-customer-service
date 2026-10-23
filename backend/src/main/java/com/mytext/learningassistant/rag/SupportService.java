package com.mytext.learningassistant.rag;

import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytext.learningassistant.common.BusinessException;
import com.mytext.learningassistant.llm.ThirdPartyLlmClient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportService {
    private final SupportConversationRepository conversations;
    private final SupportMessageRepository messages;
    private final KnowledgeBaseService knowledgeBases;
    private final ThirdPartyLlmClient llmClient;
    private final ObjectMapper objectMapper;

    public SupportService(SupportConversationRepository conversations, SupportMessageRepository messages,
                          KnowledgeBaseService knowledgeBases, ThirdPartyLlmClient llmClient, ObjectMapper objectMapper) {
        this.conversations = conversations;
        this.messages = messages;
        this.knowledgeBases = knowledgeBases;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SupportConversationResponse> listMine(long userId) {
        return conversations.findByOwnerIdOrderByUpdatedAtDesc(userId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public SupportConversationResponse getMine(long userId, long id) {
        return response(requireOwner(userId, id));
    }

    @Transactional
    public SupportConversationResponse send(long userId, Long conversationId, String content) {
        SupportConversationEntity conversation;
        if (conversationId == null) {
            conversation = new SupportConversationEntity();
            conversation.setOwnerId(userId);
            conversation = conversations.save(conversation);
        } else {
            conversation = requireOwner(userId, conversationId);
        }
        if (conversation.getStatus() == SupportStatus.CLOSED) {
            throw new BusinessException(409, "会话已关闭，请发起新会话");
        }
        String question = content.trim();
        addMessage(conversation, SupportSender.USER, question, List.of());
        if (requestsHuman(question)) {
            handoff(conversation);
            return response(conversation);
        }
        if (conversation.getStatus() == SupportStatus.WAITING_HUMAN || conversation.getStatus() == SupportStatus.HUMAN_ACTIVE) {
            return response(conversation);
        }

        if (conversation.getScenario() == null) {
            KnowledgeBaseScenario scenario = route(question);
            if (scenario == null) {
                scenario = llmClient.classifySupportScenario(userId, question)
                    .filter(result -> !"UNKNOWN".equals(result))
                    .map(KnowledgeBaseScenario::valueOf)
                    .orElse(null);
            }
            if (scenario == null) {
                addMessage(conversation, SupportSender.AI, "请说明是电商订单、配送或售后问题，还是游戏账号、充值或玩法问题。也可以直接说“转人工”。", List.of());
                return response(conversation);
            }
            conversation.setScenario(scenario);
            conversations.save(conversation);
        }

        if (requiresHumanAction(question)) {
            handoff(conversation);
            return response(conversation);
        }

        KnowledgeBaseAnswer answer;
        try {
            answer = knowledgeBases.askScenario(userId, conversation.getScenario(), question);
        } catch (BusinessException exception) {
            if (exception.getCode() != 503) throw exception;
            handoff(conversation);
            return response(conversation);
        }
        if (answer.sources().isEmpty() || needsHumanAnswer(answer.answer())) {
            handoff(conversation);
        } else {
            addMessage(conversation, SupportSender.AI, answer.answer(), answer.sources());
        }
        return response(conversation);
    }

    @Transactional
    public SupportConversationResponse requestHuman(long userId, long id) {
        SupportConversationEntity conversation = requireOwner(userId, id);
        if (conversation.getStatus() == SupportStatus.CLOSED) {
            throw new BusinessException(409, "会话已关闭，请发起新会话");
        }
        if (conversation.getStatus() == SupportStatus.AI) {
            addMessage(conversation, SupportSender.USER, "请求转人工", List.of());
        }
        handoff(conversation);
        return response(conversation);
    }

    @Transactional(readOnly = true)
    public List<SupportConversationResponse> agentQueue() {
        return conversations.findByStatusInOrderByUpdatedAtAsc(
            List.of(SupportStatus.WAITING_HUMAN, SupportStatus.HUMAN_ACTIVE)).stream()
            .map(this::response).toList();
    }

    @Transactional
    public SupportConversationResponse agentReply(long id, String content) {
        SupportConversationEntity conversation = requireConversation(id);
        if (conversation.getStatus() != SupportStatus.WAITING_HUMAN
            && conversation.getStatus() != SupportStatus.HUMAN_ACTIVE) {
            throw new BusinessException(409, "会话不在人工处理队列中");
        }
        addMessage(conversation, SupportSender.AGENT, content.trim(), List.of());
        conversation.setStatus(SupportStatus.HUMAN_ACTIVE);
        return response(conversations.save(conversation));
    }

    @Transactional
    public SupportConversationResponse close(long id) {
        SupportConversationEntity conversation = requireConversation(id);
        conversation.setStatus(SupportStatus.CLOSED);
        conversation.touch();
        return response(conversations.save(conversation));
    }

    static boolean requestsHuman(String content) {
        String text = content.toLowerCase(Locale.ROOT);
        return text.contains("转人工") || text.contains("人工客服") || text.contains("真人客服")
            || text.contains("找人工") || text.contains("我要人工") || text.contains("human agent");
    }

    static boolean requiresHumanAction(String content) {
        String text = content.toLowerCase(Locale.ROOT);
        return text.contains("我的订单") || text.contains("查订单") || text.contains("帮我退款")
            || text.contains("充值不到账") || text.contains("找回账号") || text.contains("封号申诉")
            || text.contains("my order") || text.contains("my account");
    }

    static boolean needsHumanAnswer(String answer) {
        String text = answer == null ? "" : answer.toLowerCase(Locale.ROOT);
        return text.isBlank() || text.contains("不知道") || text.contains("无法确认")
            || text.contains("缺少依据") || text.contains("没有足够依据")
            || text.contains("资料未提供") || text.contains("无法根据资料")
            || text.contains("无法判断") || text.contains("cannot determine");
    }

    static KnowledgeBaseScenario route(String content) {
        String text = content.toLowerCase(Locale.ROOT);
        int ecommerce = matches(text, "订单", "物流", "发货", "快递", "退货", "退款", "换货", "售后", "商品", "优惠", "促销", "满减", "折扣", "收货", "order", "shipping", "refund", "return", "delivery");
        int game = matches(text, "游戏", "账号", "封号", "封禁", "充值", "点券", "道具", "角色", "匹配", "服务器", "掉线", "副本", "战绩", "版本", "更新", "补丁", "赛季", "game", "account", "recharge", "server", "item");
        if (ecommerce == game) return null;
        return ecommerce > game ? KnowledgeBaseScenario.ECOMMERCE : KnowledgeBaseScenario.GAME;
    }

    private static int matches(String text, String... cues) {
        int count = 0;
        for (String cue : cues) if (text.contains(cue)) count++;
        return count;
    }

    private void handoff(SupportConversationEntity conversation) {
        if (conversation.getStatus() == SupportStatus.WAITING_HUMAN || conversation.getStatus() == SupportStatus.HUMAN_ACTIVE) return;
        conversation.setStatus(SupportStatus.WAITING_HUMAN);
        addMessage(conversation, SupportSender.AI, "已转交人工客服，后续回复会显示在此会话。", List.of());
    }

    private void addMessage(SupportConversationEntity conversation, SupportSender sender, String content,
                            List<RagSourceResponse> sources) {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setConversationId(conversation.getId());
        message.setSender(sender);
        message.setContent(content);
        if (!sources.isEmpty()) {
            try {
                message.setSourcesJson(objectMapper.writeValueAsString(sources));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Unable to serialize support sources", exception);
            }
        }
        messages.save(message);
        conversation.touch();
        conversations.save(conversation);
    }

    private SupportConversationEntity requireOwner(long userId, long id) {
        return conversations.findByIdAndOwnerId(id, userId)
            .orElseThrow(() -> new BusinessException(404, "客服会话不存在"));
    }

    private SupportConversationEntity requireConversation(long id) {
        return conversations.findById(id)
            .orElseThrow(() -> new BusinessException(404, "客服会话不存在"));
    }

    private SupportConversationResponse response(SupportConversationEntity conversation) {
        List<SupportMessageResponse> items = messages.findByConversationIdOrderByIdAsc(conversation.getId()).stream()
            .map(message -> new SupportMessageResponse(message.getId(), message.getSender(), message.getContent(),
                parseSources(message.getSourcesJson()), message.getCreatedAt()))
            .toList();
        return new SupportConversationResponse(conversation.getId(), conversation.getOwnerId(), conversation.getScenario(),
            conversation.getStatus(), items, conversation.getCreatedAt(), conversation.getUpdatedAt());
    }

    private List<RagSourceResponse> parseSources(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<RagSourceResponse>>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read support sources", exception);
        }
    }
}
