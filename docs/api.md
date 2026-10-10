# HTTP API

所有业务接口以 `/api` 为 context path，除注册登录和健康检查外均使用 `Authorization: Bearer <access-token>`。

## 认证

- `POST /api/v1/auth/register`、`POST /api/v1/auth/login`：`{"username":"...","password":"..."}`。
- `POST /api/v1/auth/refresh`：读取 HttpOnly refresh cookie，旧 token 立即撤销并轮换。
- `POST /api/v1/auth/logout`、`GET /api/v1/auth/me`。

access token 有效期 15 分钟；refresh token 有效期 7 天，服务端仅保存 SHA-256 摘要。

## 提示词会话

- `POST /api/v1/prompt-sessions`：创建会话，body 为 `taskType` 和 `memoryConsent`。
- `GET /api/v1/prompt-sessions`：返回最多 50 条轻量摘要，不返回对话正文和候选内容。
- `GET /api/v1/prompt-sessions/{id}`：返回需求、完整 `turns`、候选、记忆授权和乐观锁版本；`DELETE` 删除本人会话。
- `POST /api/v1/prompt-sessions/{id}/turns/stream`：body 为 `message`、`expectedVersion`，返回 POST SSE。
- `POST /api/v1/prompt-sessions/{id}/confirm/stream`：确认摘要并生成三个版本。
- `POST /api/v1/prompt-sessions/{id}/selection`：选择候选。
- `GET /api/v1/prompt-sessions/{id}/export?format=markdown|json`：导出已选候选。

追问流依次使用 `snapshot`、`question` 或 `confirmation_required`、`knowledge_sources`、`memory_committed`、`done`。

确认生成流使用：

- `generation_started`：服务端已将会话置为生成态。
- `generation_delta`：`candidateIndex`、`variant`、`attempt`、`delta`、`reset`。重试开始时 `reset=true`，客户端必须清空该版本先前的临时文本。
- `candidate`：一个版本已完成并通过最小校验；全部完成后另发 `candidates`。
- `knowledge_sources`、`memory_committed`、`done`：`done` 一定晚于同步记忆提交结果。
- `error`：生成失败或客户端版本冲突；失败会话恢复到可确认状态，不持久化半成品。

浏览器端必须使用支持 POST、Authorization Header 和 Fetch Streams 的客户端，不能直接使用原生 `EventSource`。

## 记忆

- `GET /api/v1/memory`：导出当前用户画像和长期记忆。
- `POST /api/v1/memory/consent`：`{"enabled":true|false}`。
- `DELETE /api/v1/memory`：删除并关闭全部长期记忆。

## 管理员知识库

- `POST /api/v1/admin/knowledge`：multipart 字段 `file`，支持 `.md/.markdown/.txt/.pdf`，最大 20 MB。
- `GET /api/v1/admin/knowledge`、`GET /api/v1/admin/knowledge/{id}`。
- `GET /api/v1/admin/knowledge/jobs/{jobId}`。
- `POST /api/v1/admin/knowledge/{id}/reindex`、`DELETE /api/v1/admin/knowledge/{id}`。
