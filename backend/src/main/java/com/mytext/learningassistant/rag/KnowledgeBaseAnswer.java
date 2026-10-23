package com.mytext.learningassistant.rag;

import java.util.List;

public record KnowledgeBaseAnswer(String answer, String modelName, List<RagSourceResponse> sources) {}
