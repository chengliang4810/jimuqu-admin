# solon-security

源码目录：`/Users/chengliang/code-repositories/solon/solon-projects/solon-security`

用途：认证授权、参数/Bean 校验、配置加密、Web 安全 Header/Filter、国密相关能力集合。

## 直接子模块

- `solon-security-auth` — Solon 鉴权模块。插件入口 `org.noear.solon.auth.integration.AuthPlugin`，注册 `@AuthIp`、`@AuthLogined`、`@AuthPath`、`@AuthPermissions`、`@AuthRoles` 拦截器，并接收 `AuthAdapter` Bean。
- `solon-security-validation` — 校验模块。插件入口 `org.noear.solon.validation.integration.ValidPlugin`，注册 `@Valid` 相关校验处理，支持 `@Validated`、`@NotNull`、`@Size` 等校验注解。
- `solon-security-vault` — 配置加密/解密模块。插件入口 `org.noear.solon.vault.integration.VaultPlugin`，注册 `@VaultInject` 注入器，支持 `ENC(...)` 解密。
- `solon-security-web` — Web 安全辅助模块，提供 `SecurityFilter` 及安全 Header Handler 组合能力。
- `solon-security-sm` — 国密相关能力模块，适用于 SM 算法场景。

## 关键源码与 API

- `solon-security-auth/src/main/java/org/noear/solon/auth/integration/AuthPlugin.java`：鉴权注解拦截器注册入口。
- `solon-security-auth/src/main/java/org/noear/solon/auth/AuthAdapter.java`：实现 `Filter`，可配置 `loginUrl`、`pathPrefix`、规则、processor、failure handler。
- `solon-security-auth/src/main/java/org/noear/solon/auth/AuthUtil.java`：手动鉴权 API，如 `verifyLogined`、`verifyPermissions`、`verifyRoles` 等。
- `solon-security-auth/src/main/java/org/noear/solon/auth/annotation/AuthRoles.java`：角色注解，支持 `Logical.OR/AND`。
- `solon-security-validation/src/main/java/org/noear/solon/validation/integration/ValidPlugin.java`：校验插件入口，读取 `solon.validation.validateAll`。
- `solon-security-validation/src/main/java/org/noear/solon/validation/annotation/Valid.java`：类/方法级校验入口。
- `solon-security-vault/src/main/java/org/noear/solon/vault/VaultUtils.java`：`ENC(...)` 解密工具。
- `solon-security-vault/src/main/java/org/noear/solon/vault/coder/AesVaultCoder.java`：默认 AES coder，读取 `solon.vault.password`。
- `solon-security-web/src/main/java/org/noear/solon/security/web/SecurityFilter.java`：组合多个 `Handler` 的安全过滤器。

## 配置与行为

- `solon.validation.validateAll` 默认 `false`。
- Vault 加密标记：`ENC(...)`。
- Vault 默认读取：`solon.vault.password`。
- 如果 `solon.vault.password` 为空，默认 AES coder 的加解密会直接返回原文。
- 多个 `AuthAdapter` 使用 `pathPrefix` 时，源码会按路径前缀长度倒序排序。

## 依赖选择与坑点

- 鉴权用 `solon-security-auth`，核心扩展点是提供 `AuthAdapter`，不要照搬 Spring Security。
- 校验用 `solon-security-validation`，不要默认套用 Spring Validator。
- 配置加密和 `ENC(...)` 用 `solon-security-vault`。
- Web 安全 Header/Filter 用 `solon-security-web`。
- `AesVaultCoder` 使用 `AES/ECB/PKCS5Padding`，安全性和 key 长度需要业务自行评估。
- `AuthUtil` 多 adapter 模式下，路径不匹配会抛 `Unsupported auth path`，路径前缀要设计清楚。
