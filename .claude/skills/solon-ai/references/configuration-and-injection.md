# 配置与注入

## 配置前缀

测试配置示例在：`/Users/chengliang/code-repositories/solon-ai/solon-ai/src/test/resources/app.yml`

常见配置：

```yaml
solon.ai.chat:
  llama3:
    apiUrl: "http://127.0.0.1:11434/api/chat"
    provider: "ollama"
    model: "llama3.2"

solon.ai.embed:
  bge-m3:
    apiUrl: "http://127.0.0.1:11434/api/embed"
    provider: "ollama"
    model: "bge-m3:latest"
```

Gemini/OpenAI 等 provider 需要 `apiKey`，可设置 `defaultOptions`、`proxy` 等复杂配置。

## 直接注入模型

`ChatModel` 和 `EmbeddingModel` 都有 `Properties` 构造器，支持 Solon 配置注入。

示例来源：`solon-ai/src/test/java/demo/ai/Config.java`

```java
@Configuration
public class Config {
    @Bean
    public ChatModel chatModel(@Inject("${solon.ai.chat.qwen}") ChatModel chatModel) {
        return chatModel;
    }

    @Bean
    public EmbeddingModel embeddingModel(@Inject("${solon.ai.embed.bge-m3}") EmbeddingModel embeddingModel) {
        return embeddingModel;
    }
}
```

## 手动构建

```java
ChatModel chatModel = ChatModel.of("http://127.0.0.1:11434/api/chat")
    .provider("ollama")
    .model("llama3.2")
    .build();

EmbeddingModel embeddingModel = EmbeddingModel.of("http://127.0.0.1:11434/api/embed")
    .provider("ollama")
    .model("bge-m3:latest")
    .batchSize(10)
    .build();
```

## 关键源码

- `solon-ai-core/src/main/java/org/noear/solon/ai/chat/ChatModel.java`：构造器要求 `apiUrl`、`model`，通过 `ChatDialectManager.select(config)` 选择方言。
- `solon-ai-core/src/main/java/org/noear/solon/ai/embedding/EmbeddingModel.java`：构造器要求 `apiUrl`、`model`，通过 `EmbeddingDialectManager.select(config)` 选择方言。
- `solon-ai-core/src/main/java/org/noear/solon/ai/integration/AiPlugin.java`：注册方言 Bean。

## 坑点

- `apiUrl` 要用完整接口地址，不要按其他框架习惯写成 `api_base`。
- Ollama 必须配置 `provider: "ollama"`，否则方言可能匹配不到。
- 报 `dialect(provider) no matched` 时，优先检查 provider、方言模块依赖和 `AiPlugin` 是否加载。
