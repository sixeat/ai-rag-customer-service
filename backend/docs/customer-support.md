# 电商与游戏客服

用户从 `/workspace/support` 的同一个入口提问。明确的订单、售后或游戏账号等词直接分流；其他问题由配置的模型尝试分类，仍不确定时要求用户补充场景。会话记住首次确定的场景。

管理员在知识库页面创建 `ECOMMERCE` 和 `GAME` 场景知识库，关联资料后勾选“发布到客服”。只有管理员发布的知识库会进入客服检索；普通用户的私有知识库不会被共享。资料答案携带来源，但客服界面不开放管理员原始文件。

用户说“转人工”、点击转人工、询问需要真实订单或账号操作的问题，或知识库无证据、模型不可用、模型明确表示依据不足时，会话转为 `WAITING_HUMAN`。管理员在 `/admin/support` 回复后，状态变为 `HUMAN_ACTIVE`；此后的用户消息保留在人工会话中，直到管理员关闭。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/support/conversations` | 我的会话 |
| POST | `/api/support/conversations` | 创建会话并发送首条消息 |
| GET | `/api/support/conversations/{id}` | 我的会话详情 |
| POST | `/api/support/conversations/{id}/messages` | 继续发送消息 |
| POST | `/api/support/conversations/{id}/handoff` | 主动转人工 |
| PUT | `/api/admin/knowledge-bases/{id}/publish?published=true` | 管理员发布自己的客服知识库 |
| GET | `/api/admin/support/conversations` | 人工待处理队列 |
| POST | `/api/admin/support/conversations/{id}/reply` | 人工回复 |
| POST | `/api/admin/support/conversations/{id}/close` | 关闭会话 |

新知识库需要先导入资料并等待文本解析完成。模型、Embedding、Qdrant 配置参见 `spring-ai-migration.md`；Qdrant 不可用时仍可走关键词检索。
