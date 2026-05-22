---
name: fastexcel
description: 辅助开发、配置、调试和解释 FastExcel / cn.idev.excel Excel 读写库。只要用户提到 FastExcel、cn.idev.excel、EasyExcel、@ExcelProperty、ExcelReader、ExcelWriter、ReadListener、AnalysisEventListener、Converter、ReadCellData、WriteCellData、WriteSheet、FillConfig、FillWrapper、模板填充、Excel 导入导出、下拉框、合并单元格、大数值精度、ExcelDataConvertException，或需要根据本地 Maven sources jar 确认 API 与行为，都必须使用此技能。此技能只记录通用 FastExcel 能力，不写入某个业务项目的专属规范。
---

# FastExcel 开发技能

你在辅助 FastExcel（`cn.idev.excel:fastexcel`）相关开发、导入导出、模板填充、自定义转换器和问题排查。本技能基于本地 Maven sources jar 与项目真实用法整理；若目标项目版本不同，先以目标项目版本为准。

本地源码来源：`/Users/chengliang/.m2/repository/cn/idev/excel/fastexcel/1.3.0/fastexcel-1.3.0-sources.jar`

## 首要原则

1. **先判定任务类型**：写 Excel、读 Excel、同步小数据读取、监听器大数据读取、自定义 Converter、样式/列宽/合并、模板填充、多 sheet、多表、CSV、异常处理或安全排查。
2. **优先查证源码**：不确定的 builder 方法、注解属性、监听器生命周期、Converter 方法签名、资源关闭行为，必须查本地 sources jar 或当前项目依赖版本。
3. **不要混用 Hutool Excel / Apache POI / EasyExcel 旧包名**：当前库包名是 `cn.idev.excel`，工厂类 `EasyExcel` 实际继承 `FastExcelFactory`。
4. **资源必须关闭**：`ExcelReader`、`ExcelWriter` 都有 `finish()/close()`；`doRead/doWrite/doReadSync` 会 finish，但手动 build 的 writer/reader 要显式 finish 或 try-with-resources。
5. **导入默认流式监听优先**：大文件不要 `doReadSync()` 一次性读入内存；监听器内分批处理。
6. **安全边界**：导入限制文件大小/行列数；导出注意公式注入；大数值要防 Excel 15 位精度丢失；模板和文件路径不要接受不可信路径。

## 资料入口

按任务读取对应 reference，必要时再读 sources jar：

- 源码结构与核心 API：`references/source-map.md`
- 写 Excel 与模板填充：`references/write-fill.md`
- 读 Excel 与监听器：`references/read-listener.md`
- 注解、Converter、样式与处理器：`references/annotation-converter-style.md`
- 排查与安全注意：`references/debugging-security.md`

## 工作流

### 写 Excel

1. 确认输出目标：文件、路径、`OutputStream`。
2. 确认 head 类和字段注解：`@ExcelProperty(value/index/order/converter)`。
3. 简单单 sheet 用 `EasyExcel.write(...).sheet(name).doWrite(data)`。
4. 多 sheet/模板/多次写入用 `ExcelWriter writer = EasyExcel.write(...).build()` + `WriteSheet`，最后 `finish()`。
5. 需要样式、列宽、合并、下拉框时注册对应 WriteHandler。

### 读 Excel

1. 小文件可 `doReadSync()`，大文件用 `AnalysisEventListener` 或 `ReadListener`。
2. 确认 head 类、sheetNo/sheetName、header 行数、numRows 限制。
3. 在 listener 中处理 `invokeHeadMap`、`invoke`、`onException`、`doAfterAllAnalysed`。
4. 转换异常优先看 `ExcelDataConvertException` 的 rowIndex/columnIndex。

### 自定义 Converter

1. 实现 `Converter<T>`。
2. 返回 `supportJavaTypeKey()` 和 `supportExcelTypeKey()`。
3. 实现 `convertToJavaData(...)` 和/或 `convertToExcelData(...)`。
4. 字段级指定：`@ExcelProperty(converter = XxxConverter.class)`；全局注册用 builder `registerConverter(...)`。

## 常用查证命令

```bash
# 解压 sources jar
rm -rf /tmp/fastexcel-1.3.0-src && mkdir -p /tmp/fastexcel-1.3.0-src
unzip -q ~/.m2/repository/cn/idev/excel/fastexcel/1.3.0/fastexcel-1.3.0-sources.jar -d /tmp/fastexcel-1.3.0-src

# 核心 API
grep -R "class FastExcelFactory\|class EasyExcel\|class ExcelWriter\|class ExcelReader" /tmp/fastexcel-1.3.0-src/cn/idev/excel

# 注解、Converter、监听器
grep -R "@interface ExcelProperty\|interface Converter\|interface ReadListener\|class AnalysisEventListener" /tmp/fastexcel-1.3.0-src/cn/idev/excel
```

## 输出规范

- 全程中文；Java 类名、方法名和注解保持原样。
- 涉及源码机制时引用 `path:line`。
- 不确定时说“源码中未确认”，不要套 Alibaba EasyExcel 旧版本或 Hutool POI API。
- 代码示例要包含资源关闭、异常处理和大文件边界。
- 安全相关默认提醒：导入限大小/行数、导出防公式注入、大数值转字符串、模板路径可信。
