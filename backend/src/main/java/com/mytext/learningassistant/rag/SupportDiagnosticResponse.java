package com.mytext.learningassistant.rag;

import java.util.List;
import java.util.Map;

/** Admin-only trace of one customer-support answer, without raw prompts or credentials. */
public record SupportDiagnosticResponse(
    String question,
    KnowledgeBaseScenario scenario,
    String outcome,
    String answer,
    List<RagSourceResponse> sources,
    int activeMaterials,
    int candidateChunks,
    Map<String, Long> contentFilters,
    String difficulty,
    String requestedTier,
    String actualTier,
    String modelName,
    boolean fallback,
    Integer totalTokens,
    long elapsedMillis
) {}
