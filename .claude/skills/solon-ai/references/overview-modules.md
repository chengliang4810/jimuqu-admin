# Solon AI 模块总览

源码目录：`/Users/chengliang/code-repositories/solon-ai`

## 根目录模块

- `solon-ai-core` — 核心 API：`ChatModel`、`EmbeddingModel`、`GenerateModel`、`RerankingModel`、RAG、工具、Skill、会话等。
- `solon-ai` — 聚合/示例测试模块，包含常用配置和测试样例。
- `solon-ai-llm-dialects` — provider 方言适配：OpenAI、Ollama、DashScope、Gemini、Anthropic。
- `solon-ai-rag-loaders` — 文档加载器：DDL、Excel、HTML、Markdown、PDF、PPT、Word。
- `solon-ai-rag-repositorys` — 向量库/知识库 Repository 适配。
- `solon-ai-rag-searchs` — Web search Repository：Baidu、Bocha、Tavily。
- `solon-ai-mcp` / `mcp-sdk` — MCP 客户端、服务端、协议 SDK。
- `solon-ai-agent` — `SimpleAgent`、`ReActAgent`、`TeamAgent` 和 Agent session/interceptor。
- `solon-ai-flow` — AI Flow YAML/组件编排。
- `solon-ai-skills` — 内置技能集合，如 browser、cli、file、memory、pdf、restapi 等。
- `solon-ai-a2a`、`solon-ai-acp`、`acp-sdk`、`solon-ai-anp` — Agent 协议相关模块。
- `solon-ai-ui` — UI 协议/界面适配模块。
- `solon-ai-harness` — harness 支持模块。

## 插件入口

- `solon-ai-core/src/main/resources/META-INF/solon/solon-ai-core.properties` → `org.noear.solon.ai.integration.AiPlugin`
- `solon-ai-mcp/src/main/resources/META-INF/solon/solon-ai-mcp.properties` → MCP 插件
- `solon-ai-flow/src/main/resources/META-INF/solon/solon-ai-flow.properties` → Flow 插件

`AiPlugin` 会收集并注册这些方言 Bean：

- `ChatDialect`
- `GenerateDialect`
- `EmbeddingDialect`
- `RerankingDialect`

## 选模块顺序

1. 普通 LLM 调用：`solon-ai-core` + 一个 provider dialect。
2. RAG：再加 loader、repository、search、reranking 等模块。
3. MCP：加 `solon-ai-mcp`，必要时看 `mcp-sdk`。
4. Agent：加 `solon-ai-agent`。
5. Flow：加 `solon-ai-flow`。
6. 内置技能：按能力从 `solon-ai-skills` 选择。

## 常见误区

- 不要把 `solon-ai` 当作唯一入口；真正核心 API 在 `solon-ai-core`。
- provider 能否匹配取决于方言模块是否在依赖中、`provider` 配置是否正确。
- RAG loader 和 vector repository 是独立模块，使用 PDF/Milvus/Qdrant 等必须引入对应模块。
