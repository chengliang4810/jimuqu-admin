# 源码结构与核心 API

## 基本信息

当前参考版本：`cn.idev.excel:fastexcel:1.3.0`

本地源码包：

```text
~/.m2/repository/cn/idev/excel/fastexcel/1.3.0/fastexcel-1.3.0-sources.jar
```

Maven 依赖：

```xml
<dependency>
    <groupId>cn.idev.excel</groupId>
    <artifactId>fastexcel</artifactId>
    <version>1.3.0</version>
</dependency>
```

## 包结构

核心包：

- `cn.idev.excel`：`EasyExcel`、`FastExcelFactory`、`ExcelReader`、`ExcelWriter`。
- `cn.idev.excel.annotation`：`@ExcelProperty`、`@ExcelIgnore`、`@ExcelIgnoreUnannotated`。
- `cn.idev.excel.annotation.format`：`@DateTimeFormat`、`@NumberFormat`。
- `cn.idev.excel.annotation.write.style`：列宽、行高、字体、样式、合并等注解。
- `cn.idev.excel.read`：读 builder、metadata、holder、processor、listener。
- `cn.idev.excel.write`：写 builder、metadata、handler、style、merge、executor。
- `cn.idev.excel.converters`：Converter SPI 和内置转换器。
- `cn.idev.excel.event`：监听器基类。
- `cn.idev.excel.metadata`：Head、CellData、ReadCellData、WriteCellData、GlobalConfiguration 等。

## EasyExcel 与 FastExcelFactory

源码：`cn/idev/excel/EasyExcel.java`

```java
public class EasyExcel extends FastExcelFactory {}
```

所以 `EasyExcel` 是 `FastExcelFactory` 的短名门面。不要把它误认为 Alibaba 旧包名 `com.alibaba.excel.EasyExcel`。

## FastExcelFactory

源码：`cn/idev/excel/FastExcelFactory.java`

写入入口：

```java
EasyExcel.write();
EasyExcel.write(File file);
EasyExcel.write(File file, Class head);
EasyExcel.write(String pathName);
EasyExcel.write(String pathName, Class head);
EasyExcel.write(OutputStream outputStream);
EasyExcel.write(OutputStream outputStream, Class head);
```

Sheet / Table builder：

```java
EasyExcel.writerSheet();
EasyExcel.writerSheet(Integer sheetNo);
EasyExcel.writerSheet(String sheetName);
EasyExcel.writerSheet(Integer sheetNo, String sheetName);
EasyExcel.writerTable();
EasyExcel.writerTable(Integer tableNo);
```

读取入口：

```java
EasyExcel.read();
EasyExcel.read(File file);
EasyExcel.read(File file, ReadListener listener);
EasyExcel.read(File file, Class head, ReadListener listener);
EasyExcel.read(String pathName, Class head, ReadListener listener);
EasyExcel.read(InputStream inputStream, Class head, ReadListener listener);
```

读取 Sheet builder：

```java
EasyExcel.readSheet();
EasyExcel.readSheet(Integer sheetNo);
EasyExcel.readSheet(String sheetName);
EasyExcel.readSheet(Integer sheetNo, String sheetName);
EasyExcel.readSheet(Integer sheetNo, String sheetName, Integer numRows);
```

注意：sheetNo 从 0 开始。

## ExcelWriter

源码：`cn/idev/excel/ExcelWriter.java`

核心方法：

```java
writer.write(Collection<?> data, WriteSheet writeSheet);
writer.write(Collection<?> data, WriteSheet writeSheet, WriteTable writeTable);
writer.fill(Object data, WriteSheet writeSheet);
writer.fill(Object data, FillConfig fillConfig, WriteSheet writeSheet);
writer.finish();
writer.close();
```

行为：

- `finish()` 会关闭 IO，释放缓存。
- `close()` 调用 `finish()`。
- 手动 `build()` 出来的 writer 必须 finish/close。

## ExcelReader

源码：`cn/idev/excel/ExcelReader.java`

核心方法：

```java
reader.readAll();
reader.read(ReadSheet... sheets);
reader.read(List<ReadSheet> sheets);
reader.analysisContext();
reader.excelExecutor();
reader.finish();
reader.close();
```

行为：

- 读取采用事件模式。
- `readAll()` 解析所有 sheet。
- `read(ReadSheet...)` 解析指定 sheet。
- `finish()` 释放缓存并关闭流。

## Builder 终止方法

`ExcelWriterSheetBuilder`：

- `build()`：只构建 `WriteSheet`。
- `doWrite(Collection<?>)`：写入并 `finish()`。
- `doFill(Object)`：填充并 `finish()`。

`ExcelReaderSheetBuilder`：

- `build()`：只构建 `ReadSheet`。
- `doRead()`：读取并 `finish()`。
- `doReadSync()`：注册 `SyncReadListener`，读取后返回 List，并 `finish()`。

## 常见选择

- 单 sheet 简单导出：`EasyExcel.write(os, Head.class).sheet("名称").doWrite(list)`。
- 多 sheet 导出：手动 `ExcelWriter writer = EasyExcel.write(os).build()`，多个 `WriteSheet`，最后 `finish()`。
- 小文件导入：`EasyExcel.read(is).head(Head.class).sheet().doReadSync()`。
- 大文件导入：`EasyExcel.read(is, Head.class, listener).sheet().doRead()`。
- 模板填充：`EasyExcel.write(os).withTemplate(templateStream).build()` + `writer.fill(...)`。
