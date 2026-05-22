---
name: solon-ai
description: 辅助开发、迁移、调试和解释 Solon AI Java AI 应用。只要用户提到 Solon AI、solon-ai、ChatModel、EmbeddingModel、RAG、DocumentLoader、Repository、向量库、ToolMapping、Skill、MCP、McpServerEndpoint、Agent、ReActAgent、TeamAgent、AI Flow、provider 方言、Ollama/OpenAI/DashScope/Gemini/Anthropic 接入，或需要根据本地 Solon AI 源码确认 API 与机制，都必须使用此技能。
---

# Solon AI Dev Skill

你在辅助 Solon AI 项目开发。Solon AI 的 API 变化快，公开资料有限，回答和实现必须优先以本地源码为准。

默认源码目录：`/Users/chengliang/code-repositories/solon-ai`

## 首要原则

1. **先判定任务类型**：模型接入、配置注入、工具调用、RAG、向量库、MCP、Agent、Flow、问题排查、迁移改造。
2. **优先查证源码**：不确定的类、注解、配置键、provider、模块依赖，必须读源码或测试示例。
3. **不要套用 Spring AI / LangChain 习惯**：Solon AI 有自己的 `ChatModel`、`EmbeddingModel`、`ToolMapping`、`Repository`、`McpServerEndpoint`、Agent API。
4. **输出要可落地**：给 Java 代码、`app.yml` 配置、依赖模块选择、源码依据和验证方式。
5. **安全边界**：涉及工具调用、文件/命令/浏览器 skill、MCP 暴露时，提醒权限、输入校验和可见副作用。

## 资料入口

按任务读取对应 reference，必要时再读源码：

- 仓库结构和模块选择：`references/overview-modules.md`
- 配置与注入：`references/configuration-and-injection.md`
- ChatModel 与 provider 方言：`references/chat-model-and-dialects.md`
- 工具调用与 Skills：`references/tool-calling-skills.md`
- RAG 主链路：`references/rag-load-split-retrieve.md`
- 向量库模块选择：`references/vector-repositories.md`
- MCP 客户端/服务端：`references/mcp-client-server.md`
- Agent：`references/agent-react-team.md`
- AI Flow：`references/flow-yaml.md`
- 示例与测试索引：`references/tests-and-examples-map.md`

如果任务很具体，直接 grep 源码优先于泛读 reference。

## 工作流

### 开发功能

1. 查当前项目已有 Solon AI 用法和依赖。
2. 读取对应 reference，确认模块和 API 名称。
3. 必要时读取 Solon AI 测试示例，优先复用真实写法。
4. 给最小实现，不引入未请求的编排层或多 provider 抽象。
5. 验证：至少编译；如涉及模型调用，说明需要真实 apiKey/本地模型服务。

### 问题排查

1. 收集异常、配置、依赖模块、provider、apiUrl、model。
2. 对照源码确认失败点：方言选择、配置绑定、工具注册、MCP endpoint、向量库连接或 loader 依赖。
3. 给最小修复和验证命令。

### 源码机制解释

1. 定位源码定义和插件声明。
2. 读取调用链和测试示例，不只读 README。
3. 输出：结论 → 关键源码路径 → 机制流程 → 坑点。

## 常用查证命令

```bash
# 核心类
grep -R "class ChatModel\|class EmbeddingModel\|@interface ToolMapping" /Users/chengliang/code-repositories/solon-ai/solon-ai-core/src/main/java

# 插件声明
find /Users/chengliang/code-repositories/solon-ai -path '*/META-INF/solon/*.properties'

# 测试示例
find /Users/chengliang/code-repositories/solon-ai -path '*/src/test/java/*' -type f | grep 'features/ai\|demo/ai'
```

## 输出规范

- 全程中文；Java 类名、配置键和代码标识保持原样。
- 涉及源码机制时引用 `path:line`。
- 不确定时说“源码中未确认”，不要补 Spring AI 或 LangChain 的 API。
- 代码示例默认使用 Solon 注解，不使用 Spring 注解，除非用户明确在 SpringBoot 嵌入场景。
