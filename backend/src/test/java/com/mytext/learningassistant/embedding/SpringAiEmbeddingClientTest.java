package com.mytext.learningassistant.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class SpringAiEmbeddingClientTest {

    @Test
    void embedsWithSpringAiOpenAiModel() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                {"object":"list","data":[{"object":"embedding","index":0,"embedding":[0.1,0.2]}],
                "model":"test-embedding","usage":{"prompt_tokens":2,"total_tokens":2}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(response);
            }
        });
        server.start();
        try {
            SpringAiEmbeddingClient client = new SpringAiEmbeddingClient(new EmbeddingProperties(true,
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-key", "test-embedding",
                Duration.ofSeconds(5), 8, 0.35));

            List<Double> embedding = client.embedQuery("vector search").orElseThrow();
            assertThat(embedding).hasSize(2);
            assertThat(embedding.get(0)).isCloseTo(0.1, org.assertj.core.data.Offset.offset(0.000001));
            assertThat(embedding.get(1)).isCloseTo(0.2, org.assertj.core.data.Offset.offset(0.000001));
            assertThat(body.get()).contains("test-embedding", "vector search");
        } finally {
            server.stop(0);
        }
    }
}
