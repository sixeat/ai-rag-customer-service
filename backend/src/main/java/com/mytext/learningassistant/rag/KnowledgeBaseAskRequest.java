package com.mytext.learningassistant.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeBaseAskRequest(@NotBlank @Size(max = 6000) String question) {}
