---
title: 短信与邮件
description: 验证码发送、Sms4j 与 SMTP 配置
---

## 短信

短信模块基于 Sms4j，支持阿里云、腾讯云等多个供应商和同供应商多配置。`sms.blends` 中的每个键是独立供应商配置标识。

短信验证码接口为 `GET /resource/sms/code`，内置手机号格式检查与每分钟限流。供应商密钥、签名和应用 ID 必须通过外部配置注入，不能提交真实值。

## 邮件

邮件模块通过 `MailProperties` 与 `MailUtils` 提供 SMTP 发送，默认 `mail.enabled: false`。启用时需要配置主机、端口、发件人、用户名、SMTP 密码以及 TLS/SSL。

邮箱验证码接口为 `GET /resource/email/code`。如果配置了本地验证码模式，可用于开发联调；生产环境应启用真实发送并限制频率。

:::caution
配置示例中的账号和密码字段只能保存占位符。生产凭据应来自环境变量或密钥管理服务。
:::
