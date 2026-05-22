# 源码结构和模块选择

## 基本信息

根 `pom.xml`：

- `groupId`: `cn.hutool`
- `artifactId`: `hutool-parent`
- `version`: `5.8.44`
- JDK 编译目标：`8`
- 许可证：木兰宽松许可证 v2

Hutool 5.x 支持 JDK8+。

## 安装方式

全量依赖：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.44</version>
</dependency>
```

建议：

- 快速原型或项目已使用全量时，可用 `hutool-all`。
- 库、SDK、基础组件或对依赖体积敏感的服务，优先按模块引入。

## 顶层模块

- `hutool-all`：聚合所有模块。
- `hutool-bom`：BOM 管理。
- `hutool-core`：核心工具，字符串、集合、Map、IO、文件、日期、Bean、转换、反射、正则、线程、编码等。
- `hutool-aop`：JDK 动态代理和非 IOC 场景切面支持。
- `hutool-bloomFilter`：布隆过滤。
- `hutool-cache`：简单缓存。
- `hutool-captcha`：图片验证码。
- `hutool-cron`：类 crontab 定时任务。
- `hutool-crypto`：摘要、对称、非对称、签名、国密等加密封装。
- `hutool-db`：JDBC 封装，ActiveRecord 风格。
- `hutool-dfa`：DFA 多关键字匹配。
- `hutool-extra`：第三方扩展封装，如邮件、模板、Servlet、二维码、Emoji、FTP、分词等。
- `hutool-http`：基于 `HttpURLConnection` 的 HTTP 客户端。
- `hutool-json`：JSON 实现。
- `hutool-jwt`：JWT 封装。
- `hutool-log`：日志门面。
- `hutool-poi`：POI Excel/Word 封装。
- `hutool-script`：脚本执行封装。
- `hutool-setting`：Setting 配置文件和 Properties 封装。
- `hutool-socket`：NIO/AIO Socket 封装。
- `hutool-system`：系统、JVM 信息封装。
- `hutool-ai`：AI 大模型封装。

## Core 包结构

`hutool-core` 是最常用模块：

- `annotation`：注解工具、组合注解、别名注解。
- `bean`：Bean 工具、属性解析、Bean 拷贝、动态 Bean。
- `codec`：Base16/32/58/62/64、BCD、PunyCode、百分号编码等。
- `collection`：`CollUtil`、`IterUtil` 和集合封装。
- `convert`：`Convert`、`ConverterRegistry`、各种转换器。
- `date`：`DateUtil`、`DateTime`、`DatePattern`、`LocalDateTimeUtil`。
- `io`：`FileUtil`、`IoUtil`、文件类型、资源、监听等。
- `lang`：`Dict`、`Assert`、`TypeReference`、`Id`、树、单例、可变对象等。
- `map`：`MapUtil` 和各种 Map 实现。
- `net`：URL、IP、SSL、网络工具。
- `text`：字符序列、CSV、字符串查找替换、Unicode。
- `thread`：线程、锁、线程池、并发工具。
- `util`：`StrUtil`、`ObjectUtil`、`ArrayUtil`、`ReflectUtil`、`ReUtil`、`IdUtil`、`NumberUtil` 等杂项工具。

## 常用源码入口

Core：

- `hutool-core/src/main/java/cn/hutool/core/util/StrUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/text/CharSequenceUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/util/ObjectUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/convert/Convert.java`
- `hutool-core/src/main/java/cn/hutool/core/convert/ConverterRegistry.java`
- `hutool-core/src/main/java/cn/hutool/core/bean/BeanUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/bean/copier/CopyOptions.java`
- `hutool-core/src/main/java/cn/hutool/core/date/DateUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/date/DateTime.java`
- `hutool-core/src/main/java/cn/hutool/core/io/FileUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/io/IoUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/collection/CollUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/map/MapUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/util/ReflectUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/util/ReUtil.java`

扩展：

- `hutool-json/src/main/java/cn/hutool/json/JSONUtil.java`
- `hutool-json/src/main/java/cn/hutool/json/JSONObject.java`
- `hutool-http/src/main/java/cn/hutool/http/HttpUtil.java`
- `hutool-http/src/main/java/cn/hutool/http/HttpRequest.java`
- `hutool-http/src/main/java/cn/hutool/http/HttpResponse.java`
- `hutool-crypto/src/main/java/cn/hutool/crypto/SecureUtil.java`
- `hutool-jwt/src/main/java/cn/hutool/jwt/JWTUtil.java`
- `hutool-poi/src/main/java/cn/hutool/poi/excel/ExcelUtil.java`
- `hutool-setting/src/main/java/cn/hutool/setting/Setting.java`
- `hutool-cache/src/main/java/cn/hutool/cache/CacheUtil.java`
- `hutool-cron/src/main/java/cn/hutool/cron/CronUtil.java`

## 测试示例入口

- `hutool-core/src/test/java/cn/hutool/core/util/StrUtilTest.java`
- `hutool-core/src/test/java/cn/hutool/core/bean/BeanUtilTest.java`
- `hutool-core/src/test/java/cn/hutool/core/convert/ConvertTest.java`
- `hutool-core/src/test/java/cn/hutool/core/date/DateUtilTest.java`
- `hutool-json/src/test/java/cn/hutool/json/JSONUtilTest.java`
- `hutool-http/src/test/java/cn/hutool/http/HttpRequestTest.java`
- `hutool-crypto/src/test/java/cn/hutool/crypto/SecureUtilTest.java`
- `hutool-jwt/src/test/java/cn/hutool/jwt/JWTUtilTest.java`
- `hutool-poi/src/test/java/cn/hutool/poi/excel/ExcelUtilTest.java`
