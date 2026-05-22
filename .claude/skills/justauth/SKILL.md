---
name: justauth
description: Use when working with JustAuth, social login, OAuth callback handling, provider-specific authorization flow, AuthRequest/AuthCallback/AuthResponse/AuthUser, state validation, redirectUri mismatches, provider switching, scope differences, or third-party login debugging in Java projects.
---

# JustAuth 开发技能

你在辅助 JustAuth（`me.zhyd.oauth:JustAuth`）相关开发、授权登录、回调处理、provider 选择和问题排查。本技能基于本地 Maven sources jar 与真实项目用法整理；若目标项目版本不同，先以目标项目版本为准。

本地源码来源：`~/.m2/repository/me/zhyd/oauth/JustAuth/1.16.7/`

## 首要原则

1. **先判定任务类型**：生成授权链接、处理回调、换取 token、获取用户信息、provider 选择、state 校验、缓存配置、开放平台对接、Solon/Spring 集成或安全排查。
2. **优先查证源码**：不确定的 request、callback、response、user、state cache、builder 和 provider 行为，必须查本地 sources jar。
3. **先选 provider 再写代码**：不同平台的回调地址、scope、参数名和用户信息结构不同，不能把别家 provider 的写法直接套进去。
4. **state 和 redirectUri 不能省**：第三方登录必须认真处理 CSRF、防重放和回调地址一致性。
5. **最小暴露**：回调接口只接收必要参数，令牌和用户信息不要随日志输出。
6. **安全边界**：不要把用户可控参数直接拼进授权地址；不要信任回调请求中的状态、来源和任意跳转地址。

## 资料入口

按任务读取对应 reference，必要时再读源码：

- 源码结构与核心 API：`references/source-map.md`
- 登录、回调与用户信息流程：`references/oauth-flow.md`
- provider 选择与缓存：`references/provider-cache-integration.md`
- Solon/Spring 集成与常见安全排查：`references/integration-security.md`

## 工作流

### 发起授权

1. 先确定 provider 类型。
2. 构建 `AuthRequest` 或具体 provider request。
3. 设置 `AuthConfig`、`redirectUri`、`state` 和必要 scope。
4. 生成授权地址或跳转响应。

### 处理回调

1. 从回调请求中读取 code、state 等参数，并优先用 `AuthCallback` 表达回调输入。
2. 校验 state 是否匹配。
3. 换取 token、获取用户信息；回答示例里尽量显式点出 `AuthResponse`、`AuthUser`，必要时同时说明通用 `AuthRequest` 或具体 provider request。
4. 按业务决定是否绑定本地用户、创建账号或返回统一用户对象。

### provider 与缓存

1. 使用源码确认每个 provider 的 request 类名和必填参数。
2. state 缓存优先用可信、短期、可过期的存储。
3. 避免把 provider 选择逻辑写死成单一平台。

## 常用查证命令

```bash
# 解压 sources jar
rm -rf /tmp/justauth-1.16.7-src
mkdir -p /tmp/justauth-1.16.7-src
unzip -q ~/.m2/repository/me/zhyd/oauth/JustAuth/1.16.7/JustAuth-1.16.7-sources.jar -d /tmp/justauth-1.16.7-src

# 核心 API
grep -R "class AuthRequest\|class AuthDefaultRequest\|class AuthConfig\|class AuthCallback\|class AuthResponse\|class AuthUser\|interface AuthStateCache" /tmp/justauth-1.16.7-src
```

## 输出规范

- 全程中文；Java 类名、方法名和注解保持原样。
- 涉及源码机制时引用 `path:line`。
- 不确定时说“源码中未确认”，不要套用别的 OAuth SDK。
- 代码示例要突出授权、回调、state 校验和最小暴露。
