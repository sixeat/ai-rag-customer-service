package com.mytext.learningassistant.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportMessageRequest(@NotBlank @Size(max = 6000) String content) {}
