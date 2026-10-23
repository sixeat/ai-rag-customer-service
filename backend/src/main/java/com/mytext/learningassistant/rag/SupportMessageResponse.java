package com.mytext.learningassistant.rag;

import java.time.LocalDateTime;
import java.util.List;

public record SupportMessageResponse(
    Long id,
    SupportSender sender,
    String content,
    List<RagSourceResponse> sources,
    LocalDateTime createdAt
) {}
