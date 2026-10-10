# 阿里云函数计算部署

## 资源

1. 同地域创建 ACR、OSS Bucket、MongoDB 和可通过 VPC 访问的 Qdrant。
2. 开通百炼并创建 API Key；生产模型默认 `qwen-plus` 和 `text-embedding-v4`。
3. 创建 FC RAM 角色，只授予目标 OSS 前缀、日志和函数异步调用权限。API 与 Worker 的 OSS SDK 使用 `EnvironmentVariableCredentialsProvider`，读取 FC 注入的临时 `ALIBABA_CLOUD_ACCESS_KEY_ID`、`ALIBABA_CLOUD_ACCESS_KEY_SECRET`、`ALIBABA_CLOUD_SECURITY_TOKEN`，不得配置长期 AccessKey。
4. 创建 SLS Project/Logstore，API Gateway 或自定义域名启用 TLS、CORS、限流和最大请求体限制。

## 镜像

从仓库根目录构建 AMD64 镜像：

```powershell
docker build --platform linux/amd64 -f deploy/Dockerfile.api -t <acr>/prompt-api:0.1.0 .
docker build --platform linux/amd64 -f deploy/Dockerfile.mcp -t <acr>/prompt-mcp:0.1.0 .
docker build --platform linux/amd64 -f deploy/Dockerfile.worker -t <acr>/prompt-worker:0.1.0 .
```

推送 ACR 后，根据 `.env.example` 在安全环境中准备变量，先用 Serverless Devs 校验 `deploy/s.yaml`，再运行 `s deploy -y`。`deploy/s.yaml` 不创建数据库、Qdrant、OSS 或密钥；这些资源必须先存在。

```powershell
npx @serverless-devs/s verify -t deploy/s.yaml
```

API 是 Qdrant 集合 schema 的初始化方，Worker 只写入已有的知识集合，避免两个函数冷启动时并发创建同一集合。首次部署应先启动 API 并确认健康，或由基础设施流程预建版本化集合，再启用 Worker 触发。

## 必须配置

- `MONGODB_URI`、`QDRANT_HOST/PORT/API_KEY`、`OSS_ENDPOINT/BUCKET`。
- `DASHSCOPE_API_KEY`、`JWT_SECRET`（至少 32 个随机字节）、`MODEL_TOOL_CALLBACKS_ENABLED=true`。
- `RAG_WORKER_URL/TOKEN`、`MCP_SERVER_URL/TOKEN`、`ALLOWED_ORIGINS`；生产固定 `RAG_WORKER_ASYNC=true`、`JWT_COOKIE_SECURE=true`。
- 首次部署可临时设置 bootstrap admin；账号创建成功后删除 bootstrap 密码变量。

API 和 Worker 监听 `0.0.0.0:9000`。API 超时建议 300 秒、Worker 900 秒。MCP 函数配置 `sessionAffinity=MCP_SSE` 和 `/sse` 会话端点；建议最小实例数 1，成本优先环境可缩到 0，但客户端必须允许重连。

## 安全边界

- Worker URL 只允许 API 所在 VPC/函数访问，并校验 `X-Worker-Token`。本地同步调用接受任意 2xx；生产请求携带 `X-Fc-Invocation-Type: Async`，只有 FC 返回 `202` 才标记为已入队。
- MCP 通过 VPC 或 API Gateway 暴露；`MCP_SERVER_TOKEN` 非空时服务端要求 `X-MCP-Token`，API 的自定义 Spring AI SSE transport 会在握手和消息请求中携带同一令牌。
- HTTP 触发器模板为便于首次部署使用 anonymous，正式发布前必须由 API Gateway/WAF 承担公网认证、限流和上传大小限制；应用层仍执行 JWT/RBAC。
- OSS 权限限定 `knowledge/` 前缀，Mongo/Qdrant 禁止公网白名单 `0.0.0.0/0`。
- 浏览器必须通过自定义域名或 API Gateway 访问。FC 默认域名可能为响应添加 `content-disposition: attachment`，不适合作为 SPA/SSE 的正式入口。

## 发布与回滚

先部署测试函数并执行登录、SSE、记忆写入、文档入库和 MCP 冒烟测试。通过后发布 FC 版本，以别名进行小流量灰度；监控模型耗时、5xx、RAG 失败和 memory 错误。回滚时将别名切回上一函数版本，Qdrant 集合按版本命名，不做破坏性原地迁移。
