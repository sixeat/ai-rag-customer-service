package com.mytext.learningassistant.rag;

/** A deterministic expectation against the bundled demonstration knowledge. */
public record SupportEvaluationCase(
    String id,
    String group,
    String question,
    KnowledgeBaseScenario expectedScenario,
    String expectedOutcome,
    String expectedDifficulty,
    String requiredTitle,
    String expectedSourceTitle,
    String mustContain
) {}
