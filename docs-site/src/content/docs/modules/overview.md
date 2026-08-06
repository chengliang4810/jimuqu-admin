---
title: 基础模块总览
description: jimuqu-common 全部模块及其职责
---

当前 `jimuqu-common` Maven 聚合器包含 20 个模块。下表覆盖全部模块，并标明在文档中的归属。

| Maven 模块 | 职责 | 使用入口 |
| --- | --- | --- |
| `jimuqu-common-bom` | 统一公共依赖版本 | 父 POM 导入 BOM |
| `jimuqu-common-core` | 响应、异常、模型、工具、加密、脱敏、XSS 注解 | 全部业务模块 |
| `jimuqu-common-doc` | 接口文档预留模块 | 当前未启用，见[接口文档状态](/modules/api-docs/) |
| `jimuqu-common-web` | Web 配置、异常、验证、加解密、XSS、下载 | Controller 层 |
| `jimuqu-common-mybatis` | Xbatis、分页、实体基类、数据权限、AutoTable | Mapper 与 Entity |
| `jimuqu-common-security` | 路径放行与安全异常处理 | `security` 配置 |
| `jimuqu-common-satoken` | 登录态、权限提供器与 Sa-Token DAO | 认证授权 |
| `jimuqu-common-log` | 操作日志切面与登录事件 | `@Log` |
| `jimuqu-common-social` | JustAuth 与扩展登录源 | 第三方绑定/登录 |
| `jimuqu-common-excel` | 导入、导出、字典转换、动态下拉、大数据量导出 | `ExcelUtil` |
| `jimuqu-common-cache` | 本地/Redis 缓存配置与版本化命名空间 | Solon CacheService |
| `jimuqu-common-oss` | x-file-storage、本地、MinIO、S3 | 文件管理 |
| `jimuqu-common-translation` | 字典、枚举和自定义字段翻译 | `@Trans` |
| `jimuqu-common-ratelimit` | 多算法、多维度限流 | `@RateLimit` |
| `jimuqu-common-redis` | Redisson 常用操作封装 | `RedisUtils` |
| `jimuqu-common-sse` | SSE 会话、心跳与消息推送 | `SseMessageUtil` |
| `jimuqu-common-websocket` | WebSocket 会话和处理器 | `WebSocketSessionHolder` |
| `jimuqu-common-idempotent` | 重复提交拦截 | `@RepeatSubmit` |
| `jimuqu-common-sms` | Sms4j 配置 | 验证码服务 |
| `jimuqu-common-mail` | SMTP 配置和邮件发送 | `MailUtils` |

:::note
模块存在不等于功能默认开启。邮件和第三方登录默认关闭；接口文档模块当前没有启用依赖。
:::
