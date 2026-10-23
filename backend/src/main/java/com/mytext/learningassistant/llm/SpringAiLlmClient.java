package com.mytext.learningassistant.llm;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.mytext.learningassistant.config.SpringAiOpenAiApiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

/** Adapts Spring AI's chat model to the application's existing LLM contract. */
public class SpringAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiLlmClient.class);
    private static final String STREAM_FAILURE_MESSAGE =
        "AI 模型响应失败或超时，请缩短问题、减少资料内容，或改用“继续”分段生成后重试。";

    private final LlmProperties properties;
    private final OpenAiChatModel model;
    private final LlmClient compatibilityClient;

    public SpringAiLlmClient(LlmProperties properties) {
        this.properties = properties;
        // Spring AI 1.1.x's OpenAI ChatModel uses Chat Completions. Retain the
        // existing transports for the two API formats it cannot represent.
        if (properties.responsesApi() || properties.baseUrl().contains("/anthropic")) {
            this.compatibilityClient = new OpenAiCompatibleLlmClient(properties);
            this.model = null;
        } else if (properties.configured()) {
            this.compatibilityClient = null;
            OpenAiApi api = SpringAiOpenAiApiFactory.create(
                properties.baseUrl(), properties.apiKey(), properties.timeout());
            this.model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(properties.model()).build())
                .build();
        } else {
            this.compatibilityClient = null;
            this.model = null;
        }
    }

    @Override
    public Optional<LlmResult> chat(String systemPrompt, String userPrompt, List<LlmImage> images) {
        if (compatibilityClient != null) {
            return compatibilityClient.chat(systemPrompt, userPrompt, images);
        }
        if (model == null) {
            return Optional.empty();
        }
        try {
            ChatResponse response = model.call(prompt(systemPrompt, userPrompt, images));
            if (response == null || response.getResult() == null) {
                return Optional.empty();
            }
            String content = response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                return Optional.empty();
            }
            Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
            String modelName = response.getMetadata() == null ? null : response.getMetadata().getModel();
            return Optional.of(new LlmResult(content.trim(),
                modelName == null || modelName.isBlank() ? properties.model() : modelName,
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                usage == null ? null : usage.getTotalTokens()));
        } catch (Exception exception) {
            log.warn("Spring AI chat failed: model={}, message={}", properties.model(), exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String chatStream(String systemPrompt, String userPrompt, List<LlmImage> images, Consumer<String> onChunk) {
        if (compatibilityClient != null) {
            return compatibilityClient.chatStream(systemPrompt, userPrompt, images, onChunk);
        }
        if (model == null) {
            return "";
        }
        StringBuilder answer = new StringBuilder();
        try {
            model.stream(prompt(systemPrompt, userPrompt, images))
                .doOnNext(response -> {
                    if (response.getResult() == null) {
                        return;
                    }
                    String chunk = response.getResult().getOutput().getText();
                    if (chunk != null && !chunk.isEmpty()) {
                        onChunk.accept(chunk);
                        answer.append(chunk);
                    }
                })
                .blockLast(properties.timeout());
            return answer.toString();
        } catch (LlmStreamCancelledException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Spring AI stream failed: model={}, message={}", properties.model(), exception.getMessage());
            throw new LlmProviderException(STREAM_FAILURE_MESSAGE, exception);
        }
    }

    private Prompt prompt(String systemPrompt, String userPrompt, List<LlmImage> images) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt == null ? "" : systemPrompt));
        List<Media> media = new ArrayList<>();
        if (images != null) {
            for (LlmImage image : images) {
                media.add(Media.builder()
                    .mimeType(MimeTypeUtils.parseMimeType(image.mediaType()))
                    .data(new ByteArrayResource(Base64.getDecoder().decode(image.base64Data())))
                    .build());
            }
        }
        messages.add(UserMessage.builder().text(userPrompt == null ? "" : userPrompt).media(media).build());
        return new Prompt(messages);
    }
}
