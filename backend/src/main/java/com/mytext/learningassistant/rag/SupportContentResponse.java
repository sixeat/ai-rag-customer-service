package com.mytext.learningassistant.rag;

import java.time.LocalDateTime;

public record SupportContentResponse(Long id, Long materialId, SupportContentCategory category,
                                     String title, String body, boolean published,
                                     LocalDateTime startsAt, LocalDateTime endsAt,
                                     String textStatus, LocalDateTime updatedAt) {}
