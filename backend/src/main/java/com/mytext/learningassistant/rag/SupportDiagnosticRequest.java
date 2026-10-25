package com.mytext.learningassistant.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportDiagnosticRequest(
    @NotBlank @Size(max = 6000) String question,
    KnowledgeBaseScenario scenario
) {}
