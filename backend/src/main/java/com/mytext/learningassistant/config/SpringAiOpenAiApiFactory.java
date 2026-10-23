package com.mytext.learningassistant.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/** Keeps the existing endpoint and timeout semantics for Spring AI model clients. */
public final class SpringAiOpenAiApiFactory {

    private SpringAiOpenAiApiFactory() {
    }

    public static OpenAiApi create(String configuredBaseUrl, String apiKey, Duration timeout) {
        String baseUrl = configuredBaseUrl.replaceAll("/+$", "");
        if (baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
        }
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(timeout).build());
        requestFactory.setReadTimeout(timeout);
        return OpenAiApi.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
            .webClientBuilder(WebClient.builder().clientConnector(new JdkClientHttpConnector(
                HttpClient.newBuilder().connectTimeout(timeout).build())))
            .build();
    }
}
