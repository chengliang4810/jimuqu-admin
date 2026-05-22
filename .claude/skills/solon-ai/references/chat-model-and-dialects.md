# ChatModel 与 Provider 方言

## ChatModel 基础

核心源码：`solon-ai-core/src/main/java/org/noear/solon/ai/chat/ChatModel.java`

`ChatModel` 提供：

- `prompt(String content)`
- `prompt(ChatMessage... messages)`
- `prompt(List<ChatMessage> messages)`
- `prompt(Prompt prompt)`
- `call()` 同步调用
- `stream()` 响应式调用
- options 中可加入 tool、skill、session 等能力

构造时必须有：

- `config.apiUrl`
- `config.model`

方言选择：`ChatDialectManager.select(config)`。

## Provider 方言模块

源码目录：`solon-ai-llm-dialects`

- `solon-ai-dialect-openai`
- `solon-ai-dialect-ollama`
- `solon-ai-dialect-dashscope`
- `solon-ai-dialect-gemini`
- `solon-ai-dialect-anthropic`

`AiPlugin` 会收集 `ChatDialect` Bean 并注册到 `ChatDialectManager`。

## Options、schema 与 tool calling

关键源码：

- `solon-ai-core/src/main/java/org/noear/solon/ai/chat/ChatOptions.java`
- `solon-ai-core/src/main/java/org/noear/solon/ai/chat/ModelOptionsAmend.java`

常用写法：

- `options(o -> o.toolAdd(new MyTools()))` 注册工具；`toolsAdd(...)` 已废弃，优先用 `toolAdd(...)`。
- `o.autoToolCall(false)` 可只返回 tool call 而不自动执行。
- `o.tool_choice("auto"|"required"|"none"|工具名)` 控制工具选择。
- `o.outputSchema(MyVo.class)` 通过 `ToolSchemaUtil.buildOutputSchema(type)` 生成输出 schema。
- `o.response_format(map)` 设置 provider 兼容的响应格式；不要把 OpenAI 的 json_schema 写法无差别套到所有 provider。
- `o.optionSet(key, val)` 传 provider 特有选项；字符串数字/布尔在 `putAll` 时会做强类型转换。

排查默认 options 时同时看：项目 `app.yml` 的 `defaultOptions`、构造器绑定配置、方言源码如何拼请求体。

## 常用示例

```java
AssistantMessage result = chatModel.prompt("今天杭州天气怎么样？")
    .options(op -> op.toolAdd(new WeatherTools()))
    .call()
    .getMessage();

chatModel.prompt("hello").stream();
```

## 多模态

README 和测试中存在视觉/多模态 Chat 调用示例，但没有独立 `ImageModel` / `AudioModel`。遇到图片理解、视觉模型时优先查 Chat 测试，而不是臆造 `ImageModel`。

## 方言报错排查

`ChatModel` 构造时会执行：

```java
this.dialect = ChatDialectManager.select(config);
Assert.notNull(dialect, "The dialect(provider) no matched, check config or dependencies");
```

遇到 `dialect(provider) no matched`：

1. 检查配置是否有 `provider`，例如 Ollama 必须是 `provider: "ollama"`。
2. 检查是否引入对应 dialect 模块，例如 `solon-ai-dialect-ollama`。
3. 检查 `AiPlugin` 是否加载并收集 `ChatDialect` Bean。
4. grep 方言类确认 provider 匹配逻辑：`solon-ai-llm-dialects/*/src/main/java/**/*Dialect.java`。
5. 不要把 OpenAI 兼容接口直接当成 OpenAI 方言；以目标服务的真实接口路径和方言源码为准。

## 排查清单

- 是否引入了对应 provider 方言模块。
- `provider` 配置是否与方言匹配。
- `apiUrl` 是否为完整接口地址。
- `apiKey` 是否配置。
- provider 特有 `defaultOptions` 是否按源码/测试示例设置。
- 模型是否支持当前能力：stream、tool calling、vision、thinking 等。
