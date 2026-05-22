# 日期、文件与 IO

## DateUtil

源码：

- `hutool-core/src/main/java/cn/hutool/core/date/DateUtil.java`
- `hutool-core/src/main/java/cn/hutool/core/date/DateTime.java`
- `hutool-core/src/main/java/cn/hutool/core/date/DatePattern.java`
- `hutool-core/src/main/java/cn/hutool/core/date/LocalDateTimeUtil.java`

常见用法：

```java
DateTime now = DateUtil.date();
String nowStr = DateUtil.now();              // yyyy-MM-dd HH:mm:ss
String today = DateUtil.today();             // yyyy-MM-dd
DateTime dt = DateUtil.parse("2026-05-21 10:30:00");
String s = DateUtil.formatDateTime(dt);
DateTime start = DateUtil.beginOfDay(dt);
DateTime end = DateUtil.endOfDay(dt);
DateTime nextWeek = DateUtil.offsetWeek(dt, 1);
long between = DateUtil.between(start, end, DateUnit.MINUTE);
```

源码行为要点：

- `DateUtil.date()` 返回 `DateTime`。
- `DateUtil.date(Date)` 传入 null 返回 null；传入 `DateTime` 原样强转返回。
- `DateUtil.date(long)` 只支持毫秒级时间戳；秒级时间戳要自行乘 1000。
- `DateUtil.now()` 是标准日期时间字符串；`today()` 是标准日期字符串。
- `DateUtil.date(TemporalAccessor)` 支持 `LocalDateTime` 等 java.time 类型。

建议：

- 新代码若大量使用 `java.time`，优先结合 `LocalDateTimeUtil`。
- 解析外部日期时显式指定格式，避免自动解析歧义。
- 涉及时区时不要只用默认时区，明确 `ZoneId`。

## FileUtil

源码：`hutool-core/src/main/java/cn/hutool/core/io/FileUtil.java`

常见用法：

```java
File file = FileUtil.file(path);
boolean exists = FileUtil.exist(path);
String text = FileUtil.readUtf8String(file);
FileUtil.writeUtf8String(text, file);
List<File> files = FileUtil.loopFiles(dir, pathname -> pathname.getName().endsWith(".java"));
FileUtil.mkdir(dir);
FileUtil.del(file);
```

源码行为要点：

- `FileUtil.ls(path)`：path 为 null 返回 null；非目录会抛 `IORuntimeException`。
- `FileUtil.isEmpty(file)`：null 或不存在返回 true；目录无子文件为 true；文件长度 <=0 为 true。
- `walkFiles(File, Consumer<File>)`：目录递归，只处理文件；非目录直接处理。

安全注意：

- 不可信路径必须做目录约束和规范化，防路径穿越。
- 删除文件前必须确认路径归属，不要用用户输入直接 `FileUtil.del`。
- 读写文本明确 UTF-8，避免平台默认字符集。

## IoUtil

常见用法：

```java
String text = IoUtil.readUtf8(inputStream);
byte[] bytes = IoUtil.readBytes(inputStream);
long copied = IoUtil.copy(in, out);
IoUtil.close(in);
```

建议：

- 资源关闭优先使用 try-with-resources；`IoUtil.close` 适合兼容性收尾。
- 大文件不要一次性 `readBytes` 或 `readUtf8`，用流式处理。
- 网络流、上传流要设置大小限制，避免内存打爆。

## ResourceUtil

常见用法：

```java
String content = ResourceUtil.readUtf8Str("config/demo.txt");
InputStream in = ResourceUtil.getStream("classpath:demo.txt");
```

注意：

- classpath 资源和文件系统路径不是一回事。
- 打成 jar 后不能假定资源能转成普通 File。

## 压缩与文件类型

Hutool core 中还有：

- `ZipUtil`：zip/gzip/zlib 压缩解压。
- `FileTypeUtil`：文件类型识别。
- `FileNameUtil`：文件名、扩展名处理。

安全注意：

- 解压 zip 要防 Zip Slip，确保解压后路径仍在目标目录内。
- 文件类型识别不能替代安全扫描。

## 常见排查

- 日期差 8 小时：检查默认时区、数据库时区、JSON 序列化时区。
- 秒级时间戳变成 1970 年：`DateUtil.date(long)` 要毫秒级。
- 文件在 IDE 能读，jar 里读不到：检查是否把 classpath 资源当文件系统路径。
- 中文乱码：显式使用 UTF-8 读写。
- 大文件 OOM：避免一次性读入内存。
