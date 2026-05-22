# Agent：Simple、ReAct、Team

源码目录：`solon-ai-agent/src/main/java/org/noear/solon/ai/agent`

## SimpleAgent

源码：`simple/SimpleAgent.java`

定位：单次直接响应型智能体，支持指令增强、历史窗口管理、自动重试、JSON 格式约束等。

适合：普通助手、结构化输出、少量工具调用、没有复杂多步推理的任务。

## ReActAgent

源码：`react/ReActAgent.java`

定位：Reason + Act 协同推理智能体，通过思考 → 动作 → 观察循环使用外部工具解决复杂任务。内部基于 Solon Flow 计算图。

适合：需要多步推理、工具调用、观察反馈、计划/执行循环的任务。

重要组件：

- `ReasonTask`
- `ActionTask`
- `ReActTrace`
- `ReActOptions`
- `ReActInterceptor`
- HITL、StopLoop、Summarization、ToolRetry、ToolSanitizer 等拦截器

## TeamAgent

源码：`team/TeamAgent.java` 及 `team/TeamProtocols.java`

定位：多智能体协作，支持协议编排成员角色。

README 示例中使用：

```java
TeamAgent team = TeamAgent.of(chatModel)
    .name("marketing_team")
    .protocol(TeamProtocols.HIERARCHICAL)
    .agentAdd(copywriterAgent)
    .agentAdd(illustratorAgent)
    .build();
```

## Session 与 HITL

Agent session 实现：

- `InMemoryAgentSession`
- `FileAgentSession`
- `RedisAgentSession`

测试位置：`solon-ai-agent/src/test/java/features/ai/session` 和 `solon-ai-agent/src/test/java/features/ai/memory`。

常见调用风格：

```java
AgentSession session = InMemoryAgentSession.of("session_001");
agent.prompt(Prompt.of("你好"))
    .session(session)
    .call();
```

HITL / 人工介入模式有两类常见源码示例：

- ReAct 工具审批：`solon-ai-agent/src/test/java/demo/ai/react/HitlWebController.java`，通过 `HITLInterceptor().onTool(...)` 挂起大额工具，`resp.getSession().isPending()` 判断待审批，`HITL.getPendingTask(session)` 查询，`HITL.submit(session, task.getToolName(), decision)` 提交后用 `agent.prompt().session(session).call()` 续跑。
- Team/Flow 审核节点：`TeamAgentHumanInTheLoopTest.java` 风格，用 `graphAdjuster` 加入自定义审核节点，自定义 `TaskComponent` 在 `FlowContext` 中检查审批标记，未审批时 `context.stop()`，外部写回同一个 session 的 FlowContext 后再次调用恢复。

源码示例确认的 Team/Flow 模式：

1. 用 `graphAdjuster` 加入自定义审核节点。
2. 自定义 `TaskComponent` 检查 `FlowContext` 中的审批标记。
3. 未审批时调用 `context.stop()` 挂起流程。
4. 外部向同一个 session 的 `FlowContext` 写入 `audit_approved=true`。
5. 再次调用 `team.prompt().session(session).call()` 从挂起点恢复。

## 选择建议

- 简单问答/结构化输出：`SimpleAgent`。
- 多步工具推理：`ReActAgent`。
- 多角色协作：`TeamAgent`。
- 需要人审/中断/恢复：优先查 HITL 和 session 测试。

## 坑点

- ReAct/Team 都有复杂 trace 和 session 状态，不要当成一次性 `ChatModel.prompt()` 使用。
- 工具安全很重要：命令、文件、网络类工具要确认权限和审计。
- Agent 模块 API 标注较新，遇到细节必须读测试示例。
