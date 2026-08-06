---
title: 安全与 Web 基础
description: 认证放行、统一异常、接口加密、脱敏与 XSS
---

安全能力分布在 `core`、`web`、`security` 和 `satoken` 四个模块。

## 主要能力

| 能力 | 入口 |
| --- | --- |
| 登录态与权限 | Sa-Token、`LoginHelper`、`@SaCheckPermission` |
| 路径放行 | `security.excludes`、`SecurityProperties` |
| 接口加解密 | `@ApiEncrypt`、`ApiEncryptInterceptor` |
| 数据脱敏 | `@Sensitive`、`SensitiveJsonRender` |
| XSS 校验 | `@Xss`、`XssInterceptor` |
| 统一异常 | `GlobalExceptionFilter`、`SecurityExceptionHandler` |
| 参数验证 | Solon Validation 与分组接口 |

## 接口加密

登录与注册接口使用 `@ApiEncrypt`。生产环境必须替换示例 RSA 密钥，并确保前端公私钥方向与后端配置对应。

## XSS 与放行路径

XSS 过滤默认开启，可为确实需要富文本的接口配置排除项，但排除后仍应使用可信 HTML 清洗策略。`security.excludes` 只放行验证码、登录、静态资源等必要入口，新增匿名接口需要单独做滥用风险评估。

## 响应格式

Controller 统一返回 `R<T>`，分页返回 `PageResult<T>`。业务异常使用 `ServiceException` 等项目异常，不直接把堆栈和数据库错误返回给客户端。
