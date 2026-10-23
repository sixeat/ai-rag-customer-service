package com.mytext.learningassistant.rag;

import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeBaseResponse(
    Long id,
    String name,
    String description,
    List<Long> materialIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
