# 排查与安全注意

## 包名和版本混淆

当前参考库是：

```text
cn.idev.excel:fastexcel:1.3.0
```

常见混淆：

- Alibaba EasyExcel 旧包名是 `com.alibaba.excel`。
- 当前 FastExcel 兼容 `EasyExcel` 类名，但包名是 `cn.idev.excel.EasyExcel`。
- 不要把 Hutool `ExcelUtil` / POI API 当成 FastExcel API。

## 导入解析失败

优先检查：

1. head 类字段是否有 `@ExcelProperty`。
2. 表头名称、列 index、sheetNo/sheetName 是否匹配。
3. 字段类型是否有内置 converter。
4. 自定义 converter 的 `supportJavaTypeKey` / `supportExcelTypeKey` 是否正确。
5. 日期格式是否通过 `@DateTimeFormat` 指定。
6. 是否发生 `ExcelDataConvertException`，读取 rowIndex/columnIndex 定位单元格。

## 监听器没有执行或只读部分数据

检查：

1. 是否调用了 `.sheet().doRead()` 或 reader.read(...)。
2. 是否传入 listener：`EasyExcel.read(inputStream, Head.class, listener)`。
3. `hasNext` 是否返回 false。
4. 是否设置了 `numRows` 限制。
5. `onException` 是否抛出异常导致读取提前终止。

## 资源关闭问题

规则：

- `doWrite`、`doFill`、`doRead`、`doReadSync` 会 finish。
- 手动 `.build()` 的 `ExcelWriter` / `ExcelReader` 必须 finish。
- 外部传入的 Web 响应流、上传流通常 `.autoCloseStream(false)`，由框架管理。

推荐：

```java
ExcelWriter writer = EasyExcel.write(os).autoCloseStream(false).build();
try {
    writer.write(rows, sheet);
} finally {
    writer.finish();
}
```

## 大文件内存问题

- 大文件导入不要用 `doReadSync()`。
- Listener 内分批处理并清空 batch。
- 大文件导出避免一次性构造巨大 List，可分批多次 `writer.write`。
- 自动列宽、复杂样式、合并单元格会增加内存和 CPU。

## 大数值精度

Excel 对超过 15 位数值会丢精度。典型处理：

- 身份证号、订单号、手机号、长整型 ID 导出为字符串。
- 自定义 `Converter<Long>`，超过 15 位写 `WriteCellData<String>`。
- 导入时也按字符串读再业务校验。

## 公式注入

导出用户可控字符串时，如果以以下字符开头，Excel 可能按公式执行：

- `=`
- `+`
- `-`
- `@`

处理方式：

- 前置单引号。
- 或按业务转义。
- 或写入前明确作为文本并过滤危险前缀。

## 文件上传安全

导入 Excel 时：

- 限制文件大小。
- 限制 sheet 数、行数、列数。
- 不信任扩展名和 Content-Type。
- 不把上传文件保存到用户可控路径。
- 校验表头和每行字段，错误信息包含行号列号。

## 模板安全

- 模板路径必须来自可信配置或 classpath 固定路径。
- 不允许用户直接传模板文件路径读取服务器任意文件。
- 模板填充数据要转义用户可控文本，避免公式注入。

## Converter 常见错误

- 只实现 `convertToExcelData`，导入时使用会抛不支持。
- 返回 `supportExcelTypeKey=STRING`，但读取到 NUMBER 单元格，匹配失败。
- `convertToExcelData` 对 null 没处理，构造 `BigDecimal(null)` 报错。
- converter 写成有状态单例，在并发导出时污染状态。

## Web 下载注意

- 设置正确文件名编码，避免中文文件名乱码。
- 使用 `.autoCloseStream(false)` 避免 FastExcel 提前关闭响应流。
- 异常发生后如果响应已开始写出，不能再返回 JSON 错误体。
