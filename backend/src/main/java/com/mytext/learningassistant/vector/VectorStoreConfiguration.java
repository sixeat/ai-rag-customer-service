package com.mytext.learningassistant.vector;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * 向量存储模块的 Spring 配置类。
 *
 * <p>职责：在 Spring 容器启动时，读取 application.yml 中 {@code app.vector-store.*} 前缀的配置，
 * 并创建基于 Spring AI QdrantVectorStore 的适配器。</p>
 */
@Configuration
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfiguration {

    /**
     * 创建并注册 {@link VectorStoreClient} Bean。
     *
     * @param properties   从配置文件中读取到的向量存储属性
     * @param grpcPort Qdrant gRPC 端口
     * @return Spring AI Qdrant 向量存储客户端实例
     */
    @Bean
    public VectorStoreClient vectorStoreClient(
        VectorStoreProperties properties,
        @Value("${app.vector-store.grpc-port:6334}") int grpcPort
    ) {
        return new SpringAiQdrantVectorStoreClient(properties, grpcPort);
    }
}
