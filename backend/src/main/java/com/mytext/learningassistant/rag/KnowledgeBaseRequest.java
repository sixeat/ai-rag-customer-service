package com.mytext.learningassistant.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeBaseRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    KnowledgeBaseScenario scenario
) {}
