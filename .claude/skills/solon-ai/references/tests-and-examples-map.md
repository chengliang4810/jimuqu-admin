# 测试与示例索引

## 总入口

- README：`/Users/chengliang/code-repositories/solon-ai/README_CN.md`
- 测试说明：`/Users/chengliang/code-repositories/solon-ai/TEST.md`
- 常用配置：`/Users/chengliang/code-repositories/solon-ai/solon-ai/src/test/resources/app.yml`
- 注入示例：`/Users/chengliang/code-repositories/solon-ai/solon-ai/src/test/java/demo/ai/Config.java`

## Chat / Embedding / RAG

- `solon-ai/src/test/java/features/ai`
- `solon-ai-core/src/test/java/features/ai/core`
- `solon-ai-core/src/test/java/demo/ai/core`

常见测试主题：Prompt、ChatSession、tool、schema、splitter、session。

## MCP

- `solon-ai-mcp/src/test/java/demo/ai/mcp`
- `solon-ai-mcp/src/test/java/features/ai/mcp`
- `solon-ai-agent/src/test/java/demo/ai/mcp`

## Agent

- `solon-ai-agent/src/test/java/demo/ai/agent`
- `solon-ai-agent/src/test/java/demo/ai/react`
- `solon-ai-agent/src/test/java/demo/ai/team`
- `solon-ai-agent/src/test/java/features/ai/simple`
- `solon-ai-agent/src/test/java/features/ai/react`
- `solon-ai-agent/src/test/java/features/ai/team`
- `solon-ai-agent/src/test/java/features/ai/session`

## Loader / Repository

各 loader/repository 模块通常有自己的 `src/test/java`。写具体 PDF、Word、Milvus、Qdrant、Redis 等代码前，优先找对应模块测试。

## 查找建议

```bash
find /Users/chengliang/code-repositories/solon-ai -path '*/src/test/java/*' -type f | grep 'features/ai\|demo/ai'

grep -R "PdfLoader\|InMemoryRepository\|ToolMapping\|McpServerEndpoint\|ReActAgent\|TeamAgent" /Users/chengliang/code-repositories/solon-ai/*/src/test/java
```
