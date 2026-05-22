# nami

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/nami`

用途：Nami RPC/HTTP 客户端代理体系，包含通道和编解码器适配。

## 直接子模块

- `nami` — 核心代理与 Solon 注入。插件 `org.noear.nami.integration.solon.NamiPlugin`。
- `nami-channel-http` — 默认 HTTP 通道。插件 `org.noear.nami.channel.http.integration.solon.NamiHttpPlugin`。
- `nami-channel-http-okhttp` — OkHttp 通道依赖适配，未发现独立 Solon plugin 声明。
- `nami-channel-http-hutool` — Hutool HTTP 通道。插件 `org.noear.nami.channel.http.hutool.integration.solon.NamiHttpHutoolPlugin`。
- `nami-channel-socketd` — Socket.D 通道。插件 `org.noear.nami.channel.socketd.integration.solon.NamiSocketdPlugin`。
- `nami-coder-*` — Snack、Gson、Fastjson、Jackson、Kryo、Hessian、Protostuff、ABC、Fury 等编解码器。

## 关键源码与 API

- `nami/src/main/java/org/noear/nami/integration/solon/NamiPlugin.java`：注册 `NamiClientInjector`。
- `nami/src/main/java/org/noear/nami/annotation/NamiClient.java`：字段包括 `url`、`group`、`name`、`path`、`headers`、`upstream`、`timeout`、`heartbeat`、`localFirst`、`configuration`、`fallback`、`fallbackFactory`。
- 常用注解：`@NamiClient`、`@NamiMapping`、`@NamiParam`、`@NamiBody`。

## 依赖选择与坑点

- 必须组合核心 `nami` + 至少一个 channel + 至少一个 coder。
- OkHttp 子模块主要是依赖适配，自动装配入口仍要看 HTTP channel 模块。
- 不要把 Nami 当作普通 Controller；它主要服务于客户端代理/RPC 调用。
