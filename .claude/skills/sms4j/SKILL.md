---
name: sms4j
description: Use when working with sms4j, SmsBlend, SmsFactory, SmsDao, SmsResponse, verification-code SMS, provider-specific SMS configuration, template SMS sending, Solon integration, or debugging provider response and rate-limit issues in Java projects.
---

# sms4j 开发技能

你在辅助 sms4j（`io.github.smiley4ever:sms4j` 及其相关模块）短信发送、provider 选择、配置和问题排查。本技能基于本地 Maven sources jar 与真实项目用法整理；若目标项目版本不同，先以目标项目版本为准。

本地源码来源：`~/.m2/repository/io/github/smiley4ever/sms4j/`

## 首要原则

1. **先判定任务类型**：发送短信、验证码、模板短信、provider 选择、配置注入、回调/响应、缓存、重试、Solon/Spring 集成或安全排查。
2. **优先查证源码**：不确定的 `SmsBlend`、`SmsFactory`、`SmsDao`、`SmsResponse`、配置类和 provider 行为，必须查本地 sources jar。
3. **先定 provider 再写代码**：阿里云、腾讯、华为、云片等 provider 的参数和限制不同，不能把一个平台的写法套到另一个平台。
4. **短信安全边界**：验证码要限频、限次、限时；不要把敏感验证码或模板参数写进日志。
5. **失败要可观测**：发送失败时优先看 provider 返回码、错误消息和配置项是否匹配。
6. **最小暴露**：发送接口只接收必要手机号和模板参数，不要把内部配置或密钥暴露给调用方。

## 资料入口

按任务读取对应 reference，必要时再读源码：

- 源码结构与核心 API：`references/source-map.md`
- 发送流程、配置和 provider 选择：`references/send-config-provider.md`
- Solon 集成与常见安全排查：`references/solon-security.md`

## 工作流

### 发送短信

1. 先确认 provider 和账号配置。
2. 构建 `SmsBlend` 或对应发送入口。
3. 准备手机号、模板编码和参数。
4. 调用发送方法并检查 `SmsResponse`。

### provider 与配置

1. provider 不同，配置字段可能不同，先查源码或示例。
2. 选择统一入口后，再根据平台适配具体参数。
3. 不要把 provider 配置硬编码在业务逻辑里。

### 排查

1. 先看 provider 返回码和错误信息。
2. 再看模板ID、签名、手机号格式、地区限制和频率限制。
3. 最后看 Solon/Spring 配置是否注入成功。

## 常用查证命令

```bash
# 解压 sources jar
rm -rf /tmp/sms4j-3.3.4-api-src /tmp/sms4j-3.3.4-core-src /tmp/sms4j-3.3.4-provider-src /tmp/sms4j-3.3.4-solon-src
mkdir -p /tmp/sms4j-3.3.4-api-src /tmp/sms4j-3.3.4-core-src /tmp/sms4j-3.3.4-provider-src /tmp/sms4j-3.3.4-solon-src
unzip -q ~/.m2/repository/io/github/smiley4ever/sms4j/sms4j-api/3.3.4/sms4j-api-3.3.4-sources.jar -d /tmp/sms4j-3.3.4-api-src
unzip -q ~/.m2/repository/io/github/smiley4ever/sms4j/sms4j-core/3.3.4/sms4j-core-3.3.4-sources.jar -d /tmp/sms4j-3.3.4-core-src
unzip -q ~/.m2/repository/io/github/smiley4ever/sms4j/sms4j-provider/3.3.4/sms4j-provider-3.3.4-sources.jar -d /tmp/sms4j-3.3.4-provider-src
unzip -q ~/.m2/repository/io/github/smiley4ever/sms4j/sms4j-solon-plugin/3.3.4/sms4j-solon-plugin-3.3.4-sources.jar -d /tmp/sms4j-3.3.4-solon-src

# 核心 API
grep -R "class SmsBlend\|class SmsFactory\|interface SmsDao\|class SmsResponse\|class SmsConfig" /tmp/sms4j-3.3.4-*-src
```

## 输出规范

- 全程中文；Java 类名、方法名和注解保持原样。
- 涉及源码机制时引用 `path:line`。
- 不确定时说“源码中未确认”，不要套用别的短信 SDK。
- 代码示例要突出发送、配置、响应判断和限频安全边界。
