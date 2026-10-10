# Security

- 禁止提交 API Key、JWT Secret、数据库 URI 密码、MCP/Worker token；提交前运行密钥扫描。
- memory 默认关闭，只有用户显式授权后才读取和写入；用户可导出、禁用和删除全部记忆。
- 身份隔离只使用 JWT subject，所有会话查询必须同时匹配 `id + userId`。
- 网页工具禁止私网目标、自动重定向和大响应；不提供终端、任意文件写入或下载工具。
- MCP HTTP 端点默认不应直接暴露公网，必须配置 VPC、API Gateway 或 `MCP_SERVER_TOKEN`。
- 参考项目中发现的硬编码第三方密钥不属于本项目且未被复制；该密钥应由所有者立即轮换。

发现漏洞时请私下联系维护者，不要在公开 issue 中披露凭据或可利用细节。
