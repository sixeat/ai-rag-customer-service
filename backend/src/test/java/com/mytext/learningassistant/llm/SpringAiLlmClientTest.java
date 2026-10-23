package com.mytext.learningassistant.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class SpringAiLlmClientTest {

    @Test
    void streamsIncrementalTextThroughExistingCallback() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] response = """
                data: {"id":"chatcmpl-test","object":"chat.completion.chunk","created":1,"model":"test-model","choices":[{"index":0,"delta":{"role":"assistant","content":"hello"},"finish_reason":null}]}

                data: {"id":"chatcmpl-test","object":"chat.completion.chunk","created":1,"model":"test-model","choices":[{"index":0,"delta":{"content":" world"},"finish_reason":"stop"}]}

                data: [DONE]

                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(response);
            }
        });
        server.start();
        try {
            SpringAiLlmClient client = new SpringAiLlmClient(new LlmProperties(true,
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-key", "test-model",
                "chat-completions", Duration.ofSeconds(5)));
            StringBuilder received = new StringBuilder();

            String answer = client.chatStream("system", "question", List.of(), received::append);

            assertThat(answer).isEqualTo("hello world");
            assertThat(received.toString()).isEqualTo(answer);
            assertThatThrownBy(() -> client.chatStream("system", "question", List.of(),
                chunk -> { throw new LlmStreamCancelledException("cancelled", null); }))
                .isInstanceOf(LlmStreamCancelledException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void chatUsesSpringAiModelAndPreservesUsageAndImage() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                {"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"test-model",
                "choices":[{"index":0,"message":{"role":"assistant","content":"answer"},"finish_reason":"stop"}],
                "usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(response);
            }
        });
        server.start();
        try {
            SpringAiLlmClient client = new SpringAiLlmClient(new LlmProperties(true,
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-key", "test-model",
                "chat-completions", Duration.ofSeconds(5)));

            LlmResult result = client.chat("system", "question",
                List.of(new LlmImage("aGVsbG8=", "image/png"))).orElseThrow();

            assertThat(result.content()).isEqualTo("answer");
            assertThat(result.promptTokens()).isEqualTo(3);
            assertThat(result.completionTokens()).isEqualTo(2);
            assertThat(result.totalTokens()).isEqualTo(5);
            assertThat(body.get()).contains("test-model", "question", "aGVsbG8=");
        } finally {
            server.stop(0);
        }
    }
}
