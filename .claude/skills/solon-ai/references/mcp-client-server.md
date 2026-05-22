# MCP 客户端与服务端

## 服务端注解

核心注解：`solon-ai-mcp/src/main/java/org/noear/solon/ai/mcp/server/annotation/McpServerEndpoint.java`

属性：

- `name`
- `version`，默认 `1.0.0`
- `channel`
- `mcpEndpoint`
- `heartbeatInterval`，默认 `30s`
- `enableOutputSchema`
- `sseEndpoint`、`messageEndpoint` 已废弃

Streamable 新写法优先：`channel = McpChannel.STREAMABLE` + `mcpEndpoint = "/mcp"`。不要再为新代码使用 `sseEndpoint`/`messageEndpoint`，除非是在解释历史兼容。

示例：

```java
@McpServerEndpoint(channel = McpChannel.STREAMABLE, mcpEndpoint = "/mcp")
public class MyMcpServer {
    @ToolMapping(description = "查询天气")
    public String getWeather(@Param(description = "城市") String location) {
        return "晴，25度";
    }
}
```

## 插件机制

核心源码：`solon-ai-mcp/src/main/java/org/noear/solon/ai/mcp/integration/McpPlugin.java`

插件会对 `@McpServerEndpoint` Bean：

1. 执行 `beanExtractOrProxy`。
2. 构建 `McpServerEndpointProvider`。
3. 自动加入：
   - `MethodToolProvider`
   - `MethodResourceProvider`
   - `MethodPromptProvider`
4. 加入容器生命周期。
5. 按 endpoint provider 名注册到容器。

## ResourceMapping 与 PromptMapping

源码：

- `solon-ai-mcp/src/main/java/org/noear/solon/ai/annotation/ResourceMapping.java`
- `solon-ai-mcp/src/main/java/org/noear/solon/ai/annotation/PromptMapping.java`

`@ResourceMapping` 属性：

- `uri`，必填
- `name`
- `title`
- `description`
- `meta`
- `mimeType`

`@PromptMapping` 属性：

- `name`
- `title`
- `description`
- `meta`

测试示例：`solon-ai-mcp/src/test/java/demo/ai/mcp/server/McpServerTool2.java`

常见写法：

```java
@ResourceMapping(uri = "config://app-version", description = "获取应用版本号", mimeType = "text/config")
public String appVersion() {
    return "1.0.0";
}

@PromptMapping(description = "生成关于某个主题的提问")
public String askTopic(@Param(description = "主题") String topic) {
    return "请围绕" + topic + "提出三个问题";
}
```

## 客户端

README 示例：

```java
McpClientProvider clientProvider = McpClientProvider.builder()
    .channel(McpChannel.STREAMABLE)
    .url("http://localhost:8080/mcp")
    .build();
```

## 相关注解

- `@ToolMapping`：工具。
- `@ResourceMapping`：资源。
- `@PromptMapping`：提示语。
- `@McpServerEndpoint`：MCP 服务端点。

## 坑点

- 不要用 Spring MCP 注解；Solon AI 使用自己的 MCP 注解和 provider。
- `sseEndpoint`、`messageEndpoint` 在源码中已废弃，新代码优先用 `mcpEndpoint` 和 `McpChannel.STREAMABLE`。
- 暴露工具给 MCP 时要关注副作用、鉴权和参数描述。
