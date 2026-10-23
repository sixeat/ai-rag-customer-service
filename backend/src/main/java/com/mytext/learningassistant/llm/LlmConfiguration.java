package com.mytext.learningassistant.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 模块配置类
 *
 * 负责配置和创建 LLM 客户端的 Spring Bean。
 * 使用 @EnableConfigurationProperties 注解启用 LlmProperties 配置属性绑定。
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

    /**
     * 创建并注册 LLM 客户端 Bean
     *
     * @param properties LLM 配置属性对象，包含 API 地址、密钥、模型等配置
     * @return Spring AI 聊天模型适配器
     */
    @Bean
    public LlmClient llmClient(LlmProperties properties) {
        return new SpringAiLlmClient(properties);
    }
}
