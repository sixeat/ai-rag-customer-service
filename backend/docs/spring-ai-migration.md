# Spring AI 后端迁移

后端继续使用 Spring Boot 3.5。AI 层接入 Spring AI 1.1.8，原有 `/api` 接口、资料数据库表和前端协议不变。

| 能力 | 当前实现 |
| --- | --- |
| OpenAI 兼容 Chat Completions | Spring AI `OpenAiChatModel`，经 `SpringAiLlmClient` 接入现有问答与摘要流程 |
| OpenAI 兼容 Embedding | Spring AI `OpenAiEmbeddingModel`，经 `SpringAiEmbeddingClient` 接入资料索引和检索 |
| Qdrant | Spring AI `QdrantVectorStore`，经 `SpringAiQdrantVectorStoreClient` 接入现有资料块和用户过滤 |
| Responses API、Anthropic Messages | 保留原有传输实现；Spring AI 1.1.8 的 OpenAI ChatModel 只支持 Chat Completions |
| Voyage multimodal Embedding | 保留原有传输实现，以维持 `document`/`query` 两种 `input_type` 与现有向量空间一致 |

Qdrant 适配器复用资料块中已经计算并保存的向量，避免写入 VectorStore 时再次付费生成 Embedding。它沿用旧实现的确定性 point UUID 和 `userId`、`materialId`、`chunkId` payload 字段。

## 部署步骤

1. 使用 JDK 21 构建后端，并配置 `VECTOR_STORE_ENABLED=true`、`VECTOR_STORE_BASE_URL` 与 `VECTOR_STORE_GRPC_PORT`。默认 gRPC 端口是 `6334`，与 REST 端口 `6333` 不同。容器网络需允许后端访问该端口。
2. 保持现有 `EMBEDDING_MODEL` 和 `VECTOR_STORE_COLLECTION` 不变。更换 Embedding 模型或维度时，应使用新集合并重建索引。
3. 部署后以管理员身份调用 `POST /api/admin/materials/vector-index/rebuild`，对所有已解析资料重建索引。旧 point 缺少 Spring AI QdrantVectorStore 使用的 `doc_content` 字段；重建会按相同 UUID 覆盖旧 point，并补齐该字段。任务在后台执行，完成前向量检索结果可能不完整。
4. 验证资料问答、图片问答和流式输出。使用 `LLM_API_FORMAT=chat-completions` 时走 Spring AI 模型；`responses` 配置仍可继续使用现有兼容传输。

本次迁移不修改 MySQL 表结构，也不会自动清空 Qdrant 集合。
