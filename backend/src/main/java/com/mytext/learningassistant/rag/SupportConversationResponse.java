package com.mytext.learningassistant.rag;

import java.time.LocalDateTime;
import java.util.List;

public record SupportConversationResponse(
    Long id,
    Long ownerId,
    KnowledgeBaseScenario scenario,
    SupportStatus status,
    List<SupportMessageResponse> messages,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
