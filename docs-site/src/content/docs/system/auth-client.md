---
title: 认证与客户端
description: 登录策略、验证码、注册与客户端授权配置
---

## 登录策略

`AuthStrategy` 根据请求中的 `grantType` 选择策略。当前源码提供五种实现：

| 授权类型 | 实现 | 用途 |
| --- | --- | --- |
| `password` | `PasswordAuthStrategy` | 用户名密码登录 |
| `sms` | `SmsAuthStrategy` | 手机验证码登录 |
| `email` | `EmailAuthStrategy` | 邮箱验证码登录 |
| `social` | `SocialAuthStrategy` | 第三方平台登录 |
| `xcx` | `XcxAuthStrategy` | 小程序登录 |

登录前会查询 `SysClient`，同时校验客户端状态与允许的 `grantType`。因此新增登录方式时，除了实现策略，还必须在客户端配置中允许该授权类型。

## 验证码

- `GET /auth/code`：图片验证码，可配置数学题或随机字符。
- `GET /resource/sms/code`：短信验证码，按手机号限流。
- `GET /resource/email/code`：邮箱验证码，要求邮件功能或本地验证码配置可用。

验证码保存在缓存中，并使用固定窗口限制发送频率。生产环境不要关闭验证码和限流后直接暴露登录入口。

## 客户端管理

客户端配置控制接入端的授权方式、状态和 Token 行为。典型客户端包括 PC 管理端、小程序或其他 API 调用端。

修改客户端时应验证：

1. `clientId` 与前端请求一致。
2. `grantType` 包含实际登录方式。
3. 客户端状态正常。
4. Token 超时和活跃超时符合终端风险等级。

## 注册与退出

`POST /auth/register` 只有在系统参数允许注册时生效；`POST /auth/logout` 会注销当前会话。第三方账号通过 `/auth/binding/{source}` 发起授权，通过 `/auth/social/callback` 完成绑定，并可使用 `/auth/unlock/{socialId}` 解除绑定。
