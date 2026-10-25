package com.mytext.learningassistant.rag;

import com.mytext.learningassistant.llm.LlmCompletion;

public record SupportModelRouteResult(
    LlmCompletion completion,
    SupportQuestionDifficultyClassifier.Difficulty difficulty,
    String requestedTier,
    String actualTier,
    boolean fallback
) {}
