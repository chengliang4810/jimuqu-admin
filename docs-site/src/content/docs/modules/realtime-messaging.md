---
title: SSE 与 WebSocket
description: 实时消息通道、心跳与会话管理
---

项目同时提供 SSE 和 WebSocket。SSE 默认推荐用于服务器单向通知；WebSocket 适合需要双向交互的场景。

## SSE

```yaml
sse:
  enabled: true
  path: /resource/message
  heartbeat: true
  heartbeatInterval: 60000
```

`SseEmitterManager` 管理连接，`SseMessageUtil` 向用户发送消息。心跳用于避免代理和 CDN 在无业务消息时关闭连接。

## WebSocket

```yaml
websocket:
  enabled: true
  path: /resource/websocket
  heartbeatInterval: 60000
```

`WebSocketSessionHolder` 保存会话，`WebSocketHandler` 处理连接事件。若关闭 WebSocket，前端对应开关也必须同步关闭。

## 部署注意

- 反向代理需要允许长连接并调整读取超时。
- 多实例部署要考虑会话粘性或跨节点消息分发。
- 客户端断线后使用带上限的退避重连，避免雪崩。
