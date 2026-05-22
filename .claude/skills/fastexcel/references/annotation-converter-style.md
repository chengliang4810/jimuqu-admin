# 注解、Converter、样式与处理器

## @ExcelProperty

源码：`cn/idev/excel/annotation/ExcelProperty.java`

属性：

```java
String[] value() default {""};
int index() default -1;
int order() default Integer.MAX_VALUE;
Class<? extends Converter<?>> converter() default AutoConverter.class;
@Deprecated String format() default "";
```

要点：

- `value`：表头名称；多级表头可写多个值。写入时多表头会自动合并；读取多表头时取最后一个。
- `index`：列索引，`-1` 表示按 Java class 排序。
- `order`：列顺序。
- 优先级：`index > order > default sort`。
- `converter`：强制当前字段使用指定转换器。
- `format` 已废弃，日期格式使用 `@DateTimeFormat`。

示例：

```java
@ExcelProperty(value = "用户ID", index = 0)
private Long id;

@ExcelProperty(value = "注册时间", index = 1)
@DateTimeFormat("yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;
```

## 忽略字段

常见注解：

- `@ExcelIgnore`：忽略单个字段。
- `@ExcelIgnoreUnannotated`：忽略未标注 `@ExcelProperty` 的字段。

## Converter

源码：`cn/idev/excel/converters/Converter.java`

接口方法：

```java
Class<?> supportJavaTypeKey();
CellDataTypeEnum supportExcelTypeKey();
T convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration);
WriteCellData<?> convertToExcelData(T value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration);
```

新上下文重载：

```java
T convertToJavaData(ReadConverterContext<?> context);
WriteCellData<?> convertToExcelData(WriteConverterContext<T> context);
```

默认实现会委托到旧参数方法。未实现的方法默认抛 `UnsupportedOperationException`。

字段级 converter：

```java
@ExcelProperty(value = "状态", converter = StatusConverter.class)
private Integer status;
```

全局注册：

```java
EasyExcel.write(outputStream, Head.class)
    .registerConverter(new BigNumberConverter())
    .sheet("数据")
    .doWrite(rows);
```

## 常见内置转换类型

源码包下有多类转换器：

- `string`、`integer`、`longconverter`、`doubleconverter`、`bigdecimal`、`biginteger`
- `date`、`localdate`、`localdatetime`
- `booleanconverter`
- `bytearray`、`file`、`inputstream`、`url` 图片转换

## 样式注解

常见包：`cn.idev.excel.annotation.write.style`

常见注解：

- `@ColumnWidth`
- `@ContentRowHeight`
- `@HeadRowHeight`
- `@ContentStyle`
- `@HeadStyle`
- `@ContentFontStyle`
- `@HeadFontStyle`
- `@OnceAbsoluteMerge`
- `@ContentLoopMerge`

建议：简单固定样式用注解；动态样式、复杂合并、下拉框用 handler。

## WriteHandler

常见接口：

- `WorkbookWriteHandler`
- `SheetWriteHandler`
- `RowWriteHandler`
- `CellWriteHandler`

注册：

```java
EasyExcel.write(outputStream, Head.class)
    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
    .registerWriteHandler(customHandler)
    .sheet("数据")
    .doWrite(rows);
```

常见实现：

- `LongestMatchColumnWidthStyleStrategy`：按内容适配列宽。
- `AbstractMergeStrategy`：自定义合并策略。
- 自定义 `SheetWriteHandler`：下拉框、冻结窗格、保护 sheet 等。

## 读取头信息

继承 `AnalysisEventListener` 后可覆盖：

```java
@Override
public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
    // headMap: 列索引 -> 表头文本
}
```

表头校验建议：

- 校验必需列是否存在。
- 校验列顺序时注意 index 从 0 开始。
- 多级表头读取默认取最后一层。

## 常见坑点

- `@ExcelProperty(index)` 和 `order` 同时设置时，index 优先。
- `converter` 只实现写或读其中一个方向时，另一个方向会抛不支持异常。
- `supportExcelTypeKey()` 要和实际单元格类型匹配。
- 日期格式不要再用废弃 `format`，优先 `@DateTimeFormat`。
- 自动列宽和复杂 handler 可能显著影响大导出性能。
