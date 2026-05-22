# JSON 与 HTTP

## JSONUtil

源码：

- `hutool-json/src/main/java/cn/hutool/json/JSONUtil.java`
- `hutool-json/src/main/java/cn/hutool/json/JSONObject.java`
- `hutool-json/src/main/java/cn/hutool/json/JSONArray.java`
- `hutool-json/src/main/java/cn/hutool/json/JSONConfig.java`

依赖：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-json</artifactId>
    <version>5.8.44</version>
</dependency>
```

常见用法：

```java
JSONObject obj = JSONUtil.parseObj(jsonStr);
JSONArray arr = JSONUtil.parseArray(jsonArrayStr);
String json = JSONUtil.toJsonStr(bean);
User user = JSONUtil.toBean(jsonStr, User.class);
List<User> users = JSONUtil.toList(arr, User.class);
```

源码行为要点：

- `createObj()` 返回空 `JSONObject`。
- `createArray()` 返回空 `JSONArray`。
- `parseObj(String)` new `JSONObject(jsonStr)`。
- `parseObj(Object)` 对 Bean 或 Map 转 JSONObject。
- `parseArray(Object)` 对数组或集合转 JSONArray。
- `parse(Object)`：
  - null 返回 null。
  - 已是 JSON 直接返回。
  - 字符串 trim 后按是否 JSONArray/JSONObject 解析。
  - 数组、Iterable、Iterator 转 JSONArray。
  - Bean 转 JSONObject。
- `parseObj(Object, boolean ignoreNullValue)` 的 ignoreNullValue 对 Bean/Map 有意义；如果 source 是 JSON 字符串，不影响 JSON 字符串本身。

建议：

- 需要控制 null、日期格式、键排序等行为时使用 `JSONConfig`。
- Bean/Map 转换优先确认字段名、日期格式和泛型集合类型。
- 对外部 JSON 输入要做大小限制和异常处理。

## HttpUtil / HttpRequest / HttpResponse

源码：

- `hutool-http/src/main/java/cn/hutool/http/HttpUtil.java`
- `hutool-http/src/main/java/cn/hutool/http/HttpRequest.java`
- `hutool-http/src/main/java/cn/hutool/http/HttpResponse.java`

依赖：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-http</artifactId>
    <version>5.8.44</version>
</dependency>
```

常见用法：

```java
String body = HttpUtil.get(url);
String result = HttpRequest.post(url)
    .header("Authorization", "Bearer " + token)
    .form(params)
    .timeout(5000)
    .execute()
    .body();
```

JSON 请求：

```java
String result = HttpRequest.post(url)
    .header("Content-Type", "application/json")
    .body(JSONUtil.toJsonStr(payload))
    .timeout(5000)
    .execute()
    .body();
```

文件上传：

```java
String result = HttpRequest.post(url)
    .form("file", FileUtil.file(path))
    .execute()
    .body();
```

源码行为要点：

- `HttpRequest` 基于 `HttpURLConnection`。
- 支持 `get/post/head/options/put/patch/delete/trace` 静态构造。
- `HttpRequest.of(url)` 会根据 `HttpGlobalConfig.isDecodeUrl()` 决定是否先解码再按 RFC3986 编码。
- `HttpRequest` 通过全局 `CookieManager` 保存 Cookie，再次请求会自动携带。
- 可通过 `HttpRequest.setGlobalTimeout(int)` 设置全局超时。

## HTTP 安全边界

- 访问用户提供的 URL 时要防 SSRF：限制协议、域名/IP、端口、内网地址、重定向。
- 必须设置连接/读取超时，避免线程挂死。
- 不要把 secret/token 拼到 URL 查询参数中，容易进入日志和 Referer。
- 对响应 body 做大小限制，避免内存占用过高。
- 文件下载要校验 Content-Type、大小、后缀与落盘目录。
- 上传文件名不要信任外部输入。

## 常见排查

JSON：

- 字段没映射：检查 Bean getter/setter、字段名、大小写、JSONConfig。
- null 丢失或多出来：检查 ignoreNullValue 与 JSONConfig。
- 日期格式不对：检查全局/局部日期格式配置。
- 泛型丢失：使用明确类型转换，不要只转 raw List。

HTTP：

- URL 参数被重复编码或乱码：检查 `HttpGlobalConfig.isDecodeUrl()` 和 charset。
- 请求一直卡住：检查 timeout。
- Cookie 串请求：检查全局 CookieManager。
- HTTPS 证书问题：查 SSL 配置，不要在生产无脑信任所有证书。
