# Prompt Generator Agent

一个通过逐步追问澄清真实需求、再生成多版本提示词的全栈智能体。项目基于 Java 21、Spring Boot、Spring AI、Vue 3，包含长期记忆、RAG、安全工具调用、MCP 客户端/服务端和阿里云函数计算部署配置。

## 主要能力

- 混合状态机：覆盖文本、代码、分析、图像和 Agent 场景，每轮只询问一个最关键缺口。
- 明确确认：信息充分后展示结构化摘要，用户确认后生成简洁、标准、严格三个版本。
- 实时生成：三个版本分别通过 POST SSE 流式返回，断线或模型失败不会把半成品写入会话。
- 双模型环境：本地 Ollama `gemma3:1b`，生产阿里云百炼 `qwen-plus`。
- 长短期记忆：导入 `spring-ai-agent-memory`，使用 MongoDB + Qdrant，并针对 Serverless 改为请求内同步提交。
- RAG：管理员上传 Markdown/TXT/PDF 到 OSS，独立任务函数负责切分、向量化和幂等入库。
- 安全工具：时间、计算、搜索、受限网页读取、提示词检查；不提供任意终端或文件写入。
- MCP：自研提示词工程 MCP Server，同时暴露 tools、resource 和 prompt。

## 模块

| 模块 | 作用 |
| --- | --- |
| `prompt-agent-core` | 领域模型、Mongo Repository、需求状态机、静态提示词检查 |
| `prompt-agent-server` | JWT API、提示词 Agent、SSE、memory、RAG 查询、知识库管理 |
| `prompt-agent-rag-worker` | OSS 文档解析、重叠切分、embedding、Qdrant 入库 |
| `prompt-engineering-mcp-server` | 提示词框架 MCP tools/resource/prompt |
| `libs` | 从指定项目导入并适配的 memory 三模块源码 |
| `prompt-agent-frontend` | Vue 3 用户工作台和管理员知识库界面 |

完整设计见 [架构文档](docs/architecture.md)，接口见 [API 文档](docs/api.md)，云部署见 [部署文档](docs/deployment.md)，验收场景见 [测试计划](docs/test-plan.md)。

如果希望从零理解项目技术、设计模式和面试讲法，请阅读 [项目白话说明与面试手册](docs/project-guide-and-interview.md)。

## 本地启动

前置软件：JDK 21、Maven 3.9+、Node 22+、Docker、Ollama 0.6+。

```powershell
ollama pull gemma3:1b
ollama pull nomic-embed-text
docker compose up -d
mvn test
mvn -pl prompt-agent-server -am spring-boot:run
```

另开终端启动 Worker、MCP 和前端：

```powershell
mvn -pl prompt-agent-rag-worker -am spring-boot:run
mvn -pl prompt-engineering-mcp-server -am spring-boot:run
npm --prefix prompt-agent-frontend ci
npm --prefix prompt-agent-frontend run dev
```

上述 Maven 命令中的 `-am` 会同时构建目标应用依赖的 Reactor 模块；聚合模块和库模块已配置为跳过 `spring-boot:run`，只有 `-pl` 指定的应用会启动。执行前请通过 `mvn -version` 确认 Maven 使用 JDK 21。

上述 npm 命令均从项目根目录执行，`--prefix` 会让 npm 使用 `prompt-agent-frontend` 中的 `package.json` 和 `package-lock.json`。

浏览器访问 `http://localhost:5173`。本地未配置 OSS 时，聊天和提示词生成正常工作，但管理员上传接口会明确返回 503。

## 配置

复制 `.env.example` 并通过 IDE、Shell 或容器环境注入变量。任何 API Key、JWT Secret、数据库密码都不得写入 YAML、Java、MCP JSON 或 Git。

`gemma3:1b` 的 Ollama 清单不支持原生 tool calling，因此本地默认 `MODEL_TOOL_CALLBACKS_ENABLED=false`，需求状态机、RAG 和提示词生成不受影响。生产 `qwen-plus` 默认开启该开关，加载内置安全工具及 MCP 工具；更换本地模型时，仅在模型明确支持 tools 后开启。

本地默认端口：API `8123`、MCP `8131`、RAG Worker `8132`、前端 `5173`、MongoDB `27017`、Qdrant gRPC `6334`。

API 负责初始化版本化的 Qdrant memory/knowledge 集合，Worker 复用 knowledge 集合；首次启动请先等待 API 健康，再提交文档索引任务，以避免分布式冷启动竞争建表。

## 验证

```powershell
mvn verify
npm --prefix prompt-agent-frontend test
npm --prefix prompt-agent-frontend run build
npm --prefix prompt-agent-frontend audit --omit=dev
```

项目正式目标运行时是 JDK 21。若开发机使用尚未被当前 Byte Buddy 正式支持的更新 JDK，可临时在测试命令增加 `-DargLine=-Dnet.bytebuddy.experimental=true`；生产镜像仍固定使用 Java 21。
