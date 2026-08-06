---
title: 第三方登录
description: JustAuth、账号绑定与扩展登录源
---

`jimuqu-common-social` 基于 JustAuth，提供状态缓存、统一配置和扩展请求实现。当前额外包含 Gitea、MaxKey、TopIAM、企业微信与本地认证源适配。

主配置默认关闭第三方登录：

```yaml
justauth:
  enabled: false
  address: "http://localhost:5666"
```

Gitee、GitHub 等来源需要分别配置客户端 ID、客户端密钥和回调地址。

## 绑定流程

1. 前端请求 `/auth/binding/{source}` 获取授权地址。
2. 用户在第三方平台授权。
3. 前端把授权码和 state 提交到 `/auth/social/callback`。
4. 后端校验第三方响应，并把账号绑定到当前登录用户。
5. 用户可通过 `/auth/unlock/{socialId}` 解除绑定。

回调地址必须与第三方平台登记值完全一致，并使用 HTTPS。state 必须一次性校验，以防止登录 CSRF。
