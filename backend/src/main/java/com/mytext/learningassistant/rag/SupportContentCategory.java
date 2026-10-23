package com.mytext.learningassistant.rag;

public enum SupportContentCategory {
    ECOMMERCE_PROMOTION(KnowledgeBaseScenario.ECOMMERCE),
    GAME_EVENT(KnowledgeBaseScenario.GAME),
    GAME_UPDATE(KnowledgeBaseScenario.GAME);

    private final KnowledgeBaseScenario scenario;

    SupportContentCategory(KnowledgeBaseScenario scenario) { this.scenario = scenario; }

    public KnowledgeBaseScenario scenario() { return scenario; }
}
