# 测试与验收计划

## 自动化测试

- `mvn verify`：覆盖需求状态机、12 轮上限、提示词检查、记忆隐私与存储、会话初始历史、生成中并发保护、JWT Cookie、Worker 异步响应、工具安全、RAG 确定性切片和 MCP 框架目录。
- `npm --prefix prompt-agent-frontend test`：覆盖 POST SSE 帧解析；`npm --prefix prompt-agent-frontend run build` 验证 Vue 生产构建。
- `npm --prefix prompt-agent-frontend audit --omit=dev`：生产依赖必须保持 0 个已知漏洞。
- `npx @serverless-devs/s verify -t deploy/s.yaml`：在注入部署变量后验证 Serverless Devs 3.x 模板和 FC3 组件 schema。

## 本地集成验收

1. 启动 MongoDB、Qdrant、Ollama、API、Worker、MCP 和前端。
2. 注册用户，确认 refresh token 轮换、退出撤销以及跨用户会话不可访问。
3. 分别创建 TEXT、CODE、ANALYSIS、IMAGE、AGENT 会话，验证每轮只提出一个问题，达到条件后必须确认。
4. 确认后观察三个版本的 `generation_delta`，验证重开页面能恢复完整对话、候选选择和导出结果。
5. 开启长期记忆，确认 `memory_committed` 在 `done` 前到达；关闭或删除记忆后不再召回。
6. 管理员上传 MD、TXT、PDF，验证重复文件、超限文件、伪 PDF、索引成功、重新索引和删除流程。
7. 调用 MCP tools/resource/prompt，验证错误 token 返回 401，stdio 模式标准输出不包含日志。

## 云端冒烟验收

- 三个镜像以 `linux/amd64` 构建并推送同地域 ACR，函数健康检查通过。
- API 经自定义域名完成登录和长连接 SSE；MCP 会话亲和生效。
- Worker 调用立即返回 202，随后 Mongo 任务从 PENDING/RUNNING 进入 SUCCEEDED 或带可诊断错误的 FAILED。
- 通过 SLS/指标检查模型耗时、HTTP 5xx、RAG 失败和记忆失败；灰度验证通过后再切换正式别名。
