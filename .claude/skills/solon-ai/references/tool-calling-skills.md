# 工具调用与 Skills

## ToolMapping

核心注解：`solon-ai-core/src/main/java/org/noear/solon/ai/annotation/ToolMapping.java`

属性：

- `name`
- `title`
- `description`
- `meta`，JSON 字符串，默认 `{}`
- `returnDirect`
- `resultConverter`

示例：

```java
public class WeatherTools {
    @ToolMapping(description = "查询天气")
    public String getWeather(@Param(description = "城市") String location) {
        return "晴，25度";
    }
}

AssistantMessage result = chatModel.prompt("今天北京天气？")
    .options(o -> o.toolAdd(new WeatherTools()))
    .call()
    .getMessage();
```

## ToolProvider

源码目录：`solon-ai-core/src/main/java/org/noear/solon/ai/chat/tool`

常见类：

- `ToolProvider`
- `MethodToolProvider`
- `FunctionTool`
- `ToolCallResultConverter`
- `ToolCallResultConverterDefault`

工具调用不生效时查：

1. 方法是否标注 `@ToolMapping`。
2. 工具对象是否通过 options 或 skill 注册。
3. provider 方言和模型是否支持 tool calling。
4. 参数是否有足够描述，例如 Solon `@Param(description = "...")`。
5. 测试示例：`solon-ai-core/src/test/java/features/ai/core`、`solon-ai/src/test/java/features/ai`。

## Skills

Skill 接口在 `org.noear.solon.ai.chat.skill` 包。

README 示例：

```java
Skill skill = new SkillDesc("order_expert")
    .description("订单助手")
    .isSupported(prompt -> prompt.getUserMessageContent().contains("订单"))
    .instruction(prompt -> "按订单 SOP 处理")
    .toolAdd(new OrderTools());

chatModel.prompt("我昨天的订单到哪了？")
    .options(o -> o.skillAdd(skill))
    .call();
```

## 内置 skills 模块

源码目录：`solon-ai-skills`

包含 browser、cli、file、memory、pdf、restapi、web、text2sql 等能力。涉及命令、文件、浏览器、网络请求时，要明确权限与副作用。
