# 扩展模块：Crypto、JWT、Excel、Setting、Cache、Cron 等

## Crypto

源码：

- `hutool-crypto/src/main/java/cn/hutool/crypto/SecureUtil.java`
- `hutool-crypto/src/main/java/cn/hutool/crypto/digest/Digester.java`
- `hutool-crypto/src/main/java/cn/hutool/crypto/symmetric/SymmetricCrypto.java`

依赖：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-crypto</artifactId>
    <version>5.8.44</version>
</dependency>
```

`SecureUtil` 覆盖三类能力：

- 摘要：MD5、SHA-1、SHA-256、HMAC 等。
- 对称加密：AES、DES、DESede、RC4、ZUC、SM4 等。
- 非对称/签名：RSA、DSA、SM2、签名算法等。

常见用法：

```java
String sha256 = SecureUtil.sha256(text);
String md5 = SecureUtil.md5(text);
AES aes = SecureUtil.aes(keyBytes);
String encrypted = aes.encryptHex(text);
String decrypted = aes.decryptStr(encrypted);
```

安全注意：

- 不要用 MD5/SHA1 存密码；密码存储应使用 BCrypt、PBKDF2、Argon2 等带盐慢哈希。
- AES/DES 需要明确模式、填充、IV、字符集，不能只靠默认值应付安全场景。
- 秘钥不要硬编码到仓库或前端。
- 加解密异常不要吞掉后继续使用空明文。
- 国密算法通常依赖 Bouncy Castle，注意 Provider 与版本。

## JWT

源码：`hutool-jwt/src/main/java/cn/hutool/jwt/JWTUtil.java`

依赖：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-jwt</artifactId>
    <version>5.8.44</version>
</dependency>
```

常见用途：

```java
String token = JWTUtil.createToken(payload, keyBytes);
boolean verified = JWTUtil.verify(token, keyBytes);
```

注意：

- JWT 只是签名载体，不等于会话管理。
- 服务端必须校验签名、过期时间、issuer/audience 等业务约束。
- 不要把敏感信息放进 payload，JWT 默认只是 Base64URL 编码，不是加密。

## Excel / POI

源码：

- `hutool-poi/src/main/java/cn/hutool/poi/excel/ExcelUtil.java`
- `hutool-poi/src/main/java/cn/hutool/poi/excel/ExcelReader.java`
- `hutool-poi/src/main/java/cn/hutool/poi/excel/ExcelWriter.java`
- `hutool-poi/src/main/java/cn/hutool/poi/excel/BigExcelWriter.java`

依赖：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-poi</artifactId>
    <version>5.8.44</version>
</dependency>
```

常见用法：

```java
ExcelReader reader = ExcelUtil.getReader(file);
List<Map<String, Object>> rows = reader.readAll();

ExcelWriter writer = ExcelUtil.getWriter(file);
writer.write(rows, true);
writer.close();
```

大数据写出用 `BigExcelWriter`。

注意：

- ExcelReader/Writer 使用后要关闭。
- 导入 Excel 要限制文件大小、sheet 数、行数、列数。
- 导出用户可控文本时注意公式注入：以 `=`, `+`, `-`, `@` 开头的单元格可能被 Excel 当公式执行。
- 大文件不要一次性读入所有行。

## Setting

源码：

- `hutool-setting/src/main/java/cn/hutool/setting/Setting.java`
- `hutool-setting/src/main/java/cn/hutool/setting/SettingUtil.java`

依赖：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-setting</artifactId>
    <version>5.8.44</version>
</dependency>
```

常见用法：

```java
Setting setting = new Setting("app.setting");
String host = setting.getStr("host", "db");
Integer port = setting.getInt("port", "db");
```

注意：

- 分组读取适合简单配置，不要替代成熟框架配置体系。
- 配置文件中不要放明文秘钥。

## Cache

源码：`hutool-cache/src/main/java/cn/hutool/cache/CacheUtil.java`

常见缓存类型：FIFO、LFU、LRU、Timed、Weak 等。

注意：

- 本地缓存不适合多实例共享状态。
- 必须规划容量、过期和清理策略。
- 缓存值可变时注意线程安全和污染。

## Cron

源码：`hutool-cron/src/main/java/cn/hutool/cron/CronUtil.java`

常见用法：

```java
CronUtil.schedule("*/5 * * * * *", task);
CronUtil.start();
```

注意：

- 确认表达式字段数量和秒级支持。
- 长任务要避免重叠执行。
- 分布式场景需要外部分布式锁或调度系统。

## Captcha

源码：`hutool-captcha/src/main/java/cn/hutool/captcha/CaptchaUtil.java`

常见用途：生成图片验证码。注意验证码答案应存服务端或可信缓存，不能下发给前端。

## 其它模块

- `hutool-db`：轻量 JDBC/ActiveRecord，适合简单场景；复杂 ORM 项目不要混用导致事务和连接管理混乱。
- `hutool-log`：日志门面，会自动识别日志实现。
- `hutool-extra`：封装第三方能力，使用前确认额外依赖。
- `hutool-script`：脚本执行风险高，不要执行不可信脚本。
- `hutool-dfa`：敏感词、多关键字匹配。
- `hutool-bloomFilter`：布隆过滤，有误判率，不能当精确存在性校验。
