# JustAuth 源码地图

## 关键类型

- `AuthRequest`
- `AuthDefaultRequest`
- `AuthConfig`
- `AuthCallback`
- `AuthResponse`
- `AuthUser`
- `AuthStateCache`
- `AuthRequestBuilder`

## 典型链路

1. 选择 provider。
2. 构建 request。
3. 生成授权地址。
4. 回调时校验 state。
5. 换 token。
6. 获取用户信息。

## 重点排查

- provider 是否选对。
- 回调参数是否完整。
- `redirectUri` 是否和平台配置一致。
- `state` 是否存储并校验。
- 用户信息字段是否因 provider 而不同。

## 参考源码

建议优先看：
- `AuthRequest.java`
- `AuthDefaultRequest.java`
- `AuthConfig.java`
- `AuthCallback.java`
- `AuthResponse.java`
- `AuthUser.java`
- `AuthStateCache.java`
