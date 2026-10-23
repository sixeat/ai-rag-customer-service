# 知识库问答

知识库复用现有资料、切片、Embedding 和 Qdrant 索引。资料可关联到多个知识库；删除知识库不会删除资料。问答只使用当前用户在该知识库中关联且文本状态为 `READY` 或 `PARTIAL` 的资料。

所有接口需要登录，路径前缀为 `/api/knowledge-bases`：

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/` | 列出自己的知识库 |
| POST | `/` | 创建，JSON 为 `name`、`description` |
| GET | `/{id}` | 查看知识库及关联资料 ID |
| PUT | `/{id}` | 修改名称和描述 |
| DELETE | `/{id}` | 删除知识库 |
| POST | `/{id}/materials/{materialId}` | 关联自己的资料，重复调用幂等 |
| DELETE | `/{id}/materials/{materialId}` | 移除关联 |
| POST | `/{id}/ask` | 提问，JSON 为 `{"question":"..."}` |

问答使用知识库内资料的 BM25 检索和可用时的 Qdrant 向量检索，返回 `answer`、`modelName`、`sources`。无证据时不会调用模型。Qdrant 未配置时仍可通过 BM25 检索；模型不可用时返回 503。单个知识库最多关联 50 份资料。
