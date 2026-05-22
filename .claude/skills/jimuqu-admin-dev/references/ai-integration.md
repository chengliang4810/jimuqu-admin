# AI 业务落地边界

jimuqu-admin 的 AI 业务要同时遵守项目规范和 Solon AI API。项目规范放在本技能；底层 `ChatModel`、RAG、MCP、Agent、Flow 细节用 `solon-ai` 查证。

## 现有 AI 模块

先查：

- `jimuqu-modules/jimuqu-ai/pom.xml`
- `jimuqu-modules/jimuqu-ai/src/main/java/com/jimuqu/ai/config/AiChatModelConfig.java`
- `jimuqu-modules/jimuqu-ai/src/main/java/com/jimuqu/ai/controller/AiController.java`
- `jimuqu-modules/jimuqu-ai/src/main/resources/jimuqu-ai.yml`

已有模式：

- `AiChatModelConfig` 通过 `@Inject("${classpath:jimuqu-ai.yml}")` 加载配置。
- `ChatModel` 由 `ChatConfig` 构建。
- `/ai/chat` 使用 `ChatModel.prompt(content).stream()` 和 `Flux<SseEvent>` 输出 SSE。

## Chat 接口增强

增强 `/ai/chat` 或新增 AI 对话接口时：

- 保留 Solon Controller 写法和现有 SSE 输出模式。
- 空输入、鉴权失败、限流、模型异常、超时、安全提示应转成明确响应或 SSE 事件。
- 不要在事件中返回 apiKey、完整异常堆栈、敏感上下文。
- API Key 鉴权优先复用系统 `SysApiKey` 能力。
- 用户级/接口级限流优先复用 `@RateLimit`。
- 会话历史建议记录用户消息、模型、耗时、估算 token、结束状态；流式分片不宜每片同步写库。

## AI 知识库 / RAG

“知识库文档管理”不要写成一个大 Controller，拆成三层：

1. **文件层**：上传用 x-file-storage，参数用 Solon `UploadedFile`，记录文件 id/url、大小、contentType、平台。
2. **业务记录层**：按项目 CRUD 分层记录知识库、文档、解析状态、索引状态、失败原因、创建人/部门。
3. **解析索引层**：异步任务或 Job 中用 Solon AI loader/splitter 解析，再用 `EmbeddingModel` + `Repository` 向量化和保存。

生产知识库不要默认 `InMemoryRepository`；具体 Qdrant、Milvus、Redis、PgVector、MySQL 等 repository 构造必须查 `solon-ai` 和对应源码/测试。

## MCP 暴露系统资源

新增 MCP 服务端点时：

- 默认只暴露只读、低敏资源，例如公告查询、公开配置、字典查询。
- 通过项目 Service 获取数据，不绕过业务权限直接读 Mapper。
- 工具方法内仍需权限判断、参数校验、数据范围过滤和字段脱敏。
- 写操作、文件操作、命令执行、浏览器访问等有副作用能力默认不暴露；确需暴露时必须有显式权限、审计日志和白名单。
- Solon AI MCP 注解/API 细节用 `solon-ai` 查证，不套 Spring MCP。

## AI 定时任务

例如每日运营摘要、知识库重建、会话统计：

- 调度与执行记录优先复用项目 Job 模块。
- 模型调用失败要记录失败原因和可重试状态。
- 长任务不要阻塞 Controller；通过 Job/异步处理推进业务状态。
