package com.mytext.learningassistant.rag;

import java.util.List;
import java.util.Optional;

import com.mytext.learningassistant.llm.LlmClient;
import com.mytext.learningassistant.llm.LlmCompletion;
import com.mytext.learningassistant.llm.LlmProperties;
import com.mytext.learningassistant.llm.LlmResult;
import com.mytext.learningassistant.llm.SpringAiLlmClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupportModelRouter {
    private static final Logger log = LoggerFactory.getLogger(SupportModelRouter.class);
    private final LlmClient flagship;
    private final LlmClient small;
    private final SupportQuestionDifficultyClassifier classifier;

    @Autowired
    public SupportModelRouter(LlmClient flagship, SupportRoutingProperties properties,
                              SupportQuestionDifficultyClassifier classifier) {
        this(flagship, properties.smallConfigured() ? new SpringAiLlmClient(new LlmProperties(
            true, properties.smallBaseUrl(), properties.smallApiKey(), properties.smallModel(),
            "chat-completions", properties.smallTimeout())) : null, classifier);
    }

    SupportModelRouter(LlmClient flagship, LlmClient small, SupportQuestionDifficultyClassifier classifier) {
        this.flagship = flagship;
        this.small = small;
        this.classifier = classifier;
    }

    public Optional<LlmCompletion> answer(String systemPrompt, String question, List<String> excerpts,
                                          List<RagSourceResponse> sources) {
        var difficulty = classifier.classify(question, sources);
        String prompt = "用户问题：\n" + question + "\n\n检索到的资料片段：\n" + String.join("\n\n", excerpts);
        if (difficulty == SupportQuestionDifficultyClassifier.Difficulty.LOW && small != null) {
            try {
                Optional<LlmResult> result = small.chat(systemPrompt, prompt);
                if (result.isPresent()) {
                    log.info("Support model route: difficulty=LOW, model={}", result.get().modelName());
                    return result.map(this::completion);
                }
            } catch (RuntimeException exception) {
                log.debug("Support small model failed: {}", exception.getMessage());
            }
            log.warn("Support small model unavailable; falling back to flagship model");
        }
        Optional<LlmResult> result = flagship.chat(systemPrompt, prompt);
        result.ifPresent(value -> log.info("Support model route: difficulty={}, model={}",
            difficulty, value.modelName()));
        return result.map(this::completion);
    }

    private LlmCompletion completion(LlmResult result) {
        return new LlmCompletion(result.content(), result.modelName(), result.promptTokens(),
            result.completionTokens(), result.totalTokens());
    }
}
