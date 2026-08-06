---
title: 前后端联调
description: 配套管理端、接口地址与联调检查
---

前端项目独立维护于 [jimuqu-admin-ui](https://gitee.com/chengliang4810/jimuqu-admin-ui)，当前使用单一 Ant Design Vue 管理端。后端默认监听 `5320`，开发时需要让前端 API 代理指向该地址。

## 联调顺序

1. 启动 MySQL 与 Redis。
2. 使用 `mvn solon:run -Pdev` 启动后端。
3. 在前端环境文件中配置后端 API 地址。
4. 启动前端开发服务器，先验证验证码、登录和当前用户信息。
5. 再验证菜单路由、权限码、字典和文件上传。

## 核心认证接口

| 接口 | 说明 |
| --- | --- |
| `GET /auth/code` | 获取图片验证码 |
| `POST /auth/login` | 按客户端与授权类型登录 |
| `GET /auth/codes` | 获取当前用户前端权限码 |
| `POST /auth/logout` | 退出登录 |
| `GET /system/user/getInfo` | 获取当前用户信息 |
| `GET /system/menu/getRouters` | 获取动态路由 |

登录请求由 `@ApiEncrypt` 处理。前后端 RSA 配置必须成对匹配，否则登录请求会在进入认证策略前失败。

## 常见问题

- **登录提示客户端异常**：检查 `clientId`、`grantType` 与客户端管理中的授权类型。
- **登录成功但没有菜单**：检查用户角色、角色菜单和 `/system/menu/getRouters` 返回值。
- **按钮不可见**：检查 `/auth/codes` 返回的权限标识是否与前端指令一致。
- **上传失败**：检查 `/resource/oss/upload` 权限、请求大小限制和存储平台状态。
- **推送断开**：确认前后端同时启用 SSE 或 WebSocket，并检查反向代理超时设置。
