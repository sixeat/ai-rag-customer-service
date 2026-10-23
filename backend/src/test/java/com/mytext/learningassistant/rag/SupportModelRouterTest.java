package com.mytext.learningassistant.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.mytext.learningassistant.llm.LlmClient;
import com.mytext.learningassistant.llm.LlmResult;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;

class SupportModelRouterTest {
    private final SupportQuestionDifficultyClassifier classifier = new SupportQuestionDifficultyClassifier();
    private final List<RagSourceResponse> sources = List.of(
        new RagSourceResponse(1L, 2L, "优惠公告", null, "优惠于周日结束。", 0.7));

    @Test
    void simpleQuestionUsesSmallModel() {
        LlmClient flagship = mock(LlmClient.class);
        LlmClient small = mock(LlmClient.class);
        when(small.chat(anyString(), anyString())).thenReturn(Optional.of(new LlmResult("周日结束", "small")));

        var answer = new SupportModelRouter(flagship, small, classifier)
            .answer("只根据资料回答", "优惠什么时候结束？", List.of("周日结束"), sources).orElseThrow();

        assertThat(answer.modelName()).isEqualTo("small");
        verify(flagship, never()).chat(anyString(), anyString());
    }

    @Test
    void complexQuestionUsesFlagshipAndSmallFailureFallsBack() {
        LlmClient flagship = mock(LlmClient.class);
        LlmClient small = mock(LlmClient.class);
        when(flagship.chat(anyString(), anyString())).thenReturn(Optional.of(new LlmResult("依据公告处理", "flagship")));
        when(small.chat(anyString(), anyString())).thenReturn(Optional.empty());
        SupportModelRouter router = new SupportModelRouter(flagship, small, classifier);

        assertThat(router.answer("依据资料", "优惠与会员券能否叠加，如果退款又怎么算？",
            List.of("公告"), sources).orElseThrow().modelName()).isEqualTo("flagship");
        verify(small, never()).chat(anyString(), anyString());
        assertThat(router.answer("依据资料", "优惠什么时候结束？", List.of("公告"), sources)
            .orElseThrow().modelName()).isEqualTo("flagship");
    }

    @Test
    void weakTopicMatchUsesFlagshipDespiteHighNormalizedScore() {
        LlmClient flagship = mock(LlmClient.class);
        LlmClient small = mock(LlmClient.class);
        when(flagship.chat(anyString(), anyString())).thenReturn(Optional.of(new LlmResult("缺少依据", "flagship")));
        var unrelated = List.of(new RagSourceResponse(1L, 2L, "优惠公告", null, "优惠什么时候结束", 0.8));

        assertThat(new SupportModelRouter(flagship, small, classifier)
            .answer("依据资料", "订单什么时候发货？", List.of("优惠什么时候结束"), unrelated)
            .orElseThrow().modelName()).isEqualTo("flagship");
        verify(small, never()).chat(anyString(), anyString());
    }

    @Test
    void smallModelExceptionFallsBackToFlagship() {
        LlmClient flagship = mock(LlmClient.class);
        LlmClient small = mock(LlmClient.class);
        when(small.chat(anyString(), anyString())).thenThrow(new IllegalStateException("connection refused"));
        when(flagship.chat(anyString(), anyString()))
            .thenReturn(Optional.of(new LlmResult("周日结束", "flagship")));

        var answer = new SupportModelRouter(flagship, small, classifier)
            .answer("依据资料", "优惠什么时候结束？", List.of("周日结束"), sources).orElseThrow();

        assertThat(answer.modelName()).isEqualTo("flagship");
        verify(flagship).chat(anyString(), anyString());
    }

    @Test
    void ollamaCompatibleEndpointCanServeSmallModel() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                {"id":"local-test","object":"chat.completion","created":1,"model":"qwen-local",
                "choices":[{"index":0,"message":{"role":"assistant","content":"周日结束"},"finish_reason":"stop"}]}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var stream = exchange.getResponseBody()) { stream.write(response); }
        });
        server.start();
        try {
            var properties = new SupportRoutingProperties(true,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "ollama", "qwen-local",
                Duration.ofSeconds(5));
            SupportModelRouter router = new SupportModelRouter(mock(LlmClient.class), properties, classifier);

            assertThat(router.answer("依据资料", "优惠什么时候结束？", List.of("周日结束"), sources)
                .orElseThrow().modelName()).isEqualTo("qwen-local");
            assertThat(requestBody.get()).contains("qwen-local", "优惠什么时候结束");
        } finally {
            server.stop(0);
        }
    }
}
