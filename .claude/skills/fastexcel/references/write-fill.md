# 写 Excel 与模板填充

## 单 sheet 导出

依赖：

```xml
<dependency>
    <groupId>cn.idev.excel</groupId>
    <artifactId>fastexcel</artifactId>
    <version>1.3.0</version>
</dependency>
```

DTO：

```java
public class UserExport {
    @ExcelProperty(value = "用户ID", index = 0)
    private Long id;

    @ExcelProperty(value = "用户名", index = 1)
    private String username;
}
```

写出：

```java
EasyExcel.write(outputStream, UserExport.class)
    .autoCloseStream(false)
    .sheet("用户数据")
    .doWrite(rows);
```

要点：

- `EasyExcel` 实际继承 `FastExcelFactory`。
- `doWrite` 会调用 `excelWriter.finish()`。
- `autoCloseStream(false)` 表示不由 FastExcel 关闭外部传入流，适合 Web 响应流。

## 多 sheet 写入

```java
ExcelWriter writer = EasyExcel.write(outputStream).autoCloseStream(false).build();
try {
    WriteSheet userSheet = EasyExcel.writerSheet(0, "用户").head(UserExport.class).build();
    writer.write(userRows, userSheet);

    WriteSheet roleSheet = EasyExcel.writerSheet(1, "角色").head(RoleExport.class).build();
    writer.write(roleRows, roleSheet);
} finally {
    writer.finish();
}
```

要点：

- `writerSheet(Integer sheetNo, String sheetName)` 的 sheetNo 从 0 开始。
- 手动 build 的 `ExcelWriter` 必须 `finish()`。

## 样式、列宽、合并、下拉

常见 handler：

```java
EasyExcel.write(outputStream, UserExport.class)
    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
    .registerWriteHandler(customMergeStrategy)
    .registerWriteHandler(customDropDownHandler)
    .sheet("用户")
    .doWrite(rows);
```

相关接口/类：

- `WriteHandler`
- `CellWriteHandler`
- `RowWriteHandler`
- `SheetWriteHandler`
- `WorkbookWriteHandler`
- `AbstractMergeStrategy`
- `LongestMatchColumnWidthStyleStrategy`

注意：

- 自动列宽对超大数据量可能影响性能。
- 合并单元格和下拉框通常依赖 POI 底层对象，需控制行列范围。

## 模板填充

单对象/单列表：

```java
ExcelWriter writer = EasyExcel.write(outputStream)
    .withTemplate(templateInputStream)
    .autoCloseStream(false)
    .build();
try {
    WriteSheet sheet = EasyExcel.writerSheet().build();
    writer.fill(data, sheet);
} finally {
    writer.finish();
}
```

多列表填充：

```java
FillConfig fillConfig = FillConfig.builder().forceNewRow(true).build();
writer.fill(new FillWrapper("users", userRows), fillConfig, sheet);
writer.fill(new FillWrapper("roles", roleRows), fillConfig, sheet);
```

模板占位：

- 单对象：`{name}`
- 列表：`{users.name}`，结合 `FillWrapper("users", rows)`。

注意：

- 模板路径必须可信，避免用户控制模板路径读取任意文件。
- 多列表填充要关注 `forceNewRow` 对性能和模板布局的影响。

## 大数值精度

Excel 对超过 15 位的数值容易丢精度。常见做法：自定义 `Converter<Long>`，超过 15 位写成字符串。

```java
public class BigNumberConverter implements Converter<Long> {
    @Override
    public Class<Long> supportJavaTypeKey() {
        return Long.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public WriteCellData<?> convertToExcelData(Long value, ExcelContentProperty property, GlobalConfiguration config) {
        String text = String.valueOf(value);
        if (text.length() > 15) {
            return new WriteCellData<>(text);
        }
        WriteCellData<Object> data = new WriteCellData<>(new BigDecimal(value));
        data.setType(CellDataTypeEnum.NUMBER);
        return data;
    }
}
```

注册：

```java
EasyExcel.write(outputStream, Head.class)
    .registerConverter(new BigNumberConverter())
    .sheet("数据")
    .doWrite(rows);
```

## 导出安全

- 防公式注入：用户可控字符串以 `=`, `+`, `-`, `@` 开头时，导出前转义或加前缀。
- 限制导出行数，超大导出分批或异步生成。
- Web 下载设置正确 `Content-Type`、文件名编码和缓存头。
- 不要把敏感字段无意导出。
