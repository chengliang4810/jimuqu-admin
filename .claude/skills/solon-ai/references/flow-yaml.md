# AI Flow YAML 编排

源码目录：`solon-ai-flow`

## 定位

`solon-ai-flow` 基于 `solon-flow` 构建，是 AI 流编排框架。它把 AI 能力封装为 `TaskComponent`，通过 YAML 或 JSON 串起来，风格类似 docker-compose，但目标是应用开发框架而非低代码平台。

核心理念：

- 输入输出组件：有输入输出需求。
- 属性组件：有属性添加或获取需求。
- 数据通过 `FlowContext` 共享和中转。

## 常见组件

README 示例确认的组件：

- `@VarInput`
- `@ChatModel`
- `@EmbeddingModel`
- `@InMemoryRepository`
- `@McpTool`
- `@ConsoleOutput`

## Chat 示例

```yaml
id: chat_case1
layout:
  - type: "start"
  - task: "@VarInput"
    meta:
      message: "你好"
  - task: "@ChatModel"
    meta:
      systemPrompt: "你是个聊天助手"
      stream: false
      chatConfig:
        provider: "ollama"
        model: "qwen2.5:1.5b"
        apiUrl: "http://127.0.0.1:11434/api/chat"
  - task: "@ConsoleOutput"
```

执行：

```java
flowEngine.eval("chat_case1");
```

## RAG 示例要点

RAG Flow 中常见顺序：

1. `@VarInput`
2. `@EmbeddingModel`
3. `@InMemoryRepository`
4. `@ChatModel`
5. `@ConsoleOutput`

`@InMemoryRepository` 可配置 `documentSources` 和 `splitPipeline`。

## 坑点

- Flow 配置里的 `chatConfig`、`embeddingConfig` 仍然要符合 Solon AI 模型配置。
- `toolProviders` 写类名时，要保证类可实例化/可被框架识别。
- Flow 适合固定链路编排，不要为简单单次 ChatModel 调用引入 Flow。
