# sms4j 源码地图

## 关键类型

- `SmsBlend`
- `SmsFactory`
- `SmsDao`
- `SmsResponse`
- `SmsConfig`
- provider 相关实现类

## 典型链路

1. 选择 provider。
2. 读取配置。
3. 构建发送入口。
4. 传入手机号、模板和参数。
5. 读取响应结果。

## 重点排查

- provider 是否选对。
- 模板 ID、签名、手机号格式是否正确。
- 配置是否注入成功。
- 返回码和错误信息是否指向 provider 限制。

## 参考源码

建议优先看：
- `SmsBlend.java`
- `SmsFactory.java`
- `SmsDao.java`
- `SmsResponse.java`
- `SmsConfig.java`
