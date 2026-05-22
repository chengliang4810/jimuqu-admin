# 读 Excel 与监听器

## 小文件同步读取

适合小数据量：

```java
List<UserImport> rows = EasyExcel.read(inputStream)
    .head(UserImport.class)
    .autoCloseStream(false)
    .sheet()
    .doReadSync();
```

源码行为：`ExcelReaderSheetBuilder.doReadSync()` 会注册 `SyncReadListener`，读取完成后 `excelReader.finish()` 并返回 List。

注意：

- `doReadSync()` 会把结果全部放入内存，大文件不要用。
- 外部传入流如 Web 上传流通常设置 `autoCloseStream(false)`，由外层框架管理。

## 监听器读取

```java
public class UserImportListener extends AnalysisEventListener<UserImport> {
    private final List<UserImport> batch = new ArrayList<>();

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        // 读取表头
    }

    @Override
    public void invoke(UserImport data, AnalysisContext context) {
        batch.add(data);
        if (batch.size() >= 1000) {
            saveBatch(batch);
            batch.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!batch.isEmpty()) {
            saveBatch(batch);
        }
    }
}
```

调用：

```java
EasyExcel.read(inputStream, UserImport.class, new UserImportListener())
    .autoCloseStream(false)
    .sheet()
    .doRead();
```

## ReadListener 生命周期

源码：`cn/idev/excel/read/listener/ReadListener.java`

关键方法：

- `onException(Exception exception, AnalysisContext context)`：任何 listener 报错都会触发；默认重新抛出，终止读取。
- `invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context)`：读到表头时触发。
- `invoke(T data, AnalysisContext context)`：读到一行数据时触发。
- `extra(CellExtra extra, AnalysisContext context)`：额外信息，如批注、超链接等。
- `doAfterAllAnalysed(AnalysisContext context)`：全部解析完成后触发。
- `hasNext(AnalysisContext context)`：返回 false 可停止读取；源码会结合 `numRows` 限制。

`AnalysisEventListener`：

- 实现 `ReadListener`。
- 默认把 `invokeHead` 的 `ReadCellData` 转成 `Map<Integer, String>` 后调用 `invokeHeadMap`。
- 自定义监听器通常继承它。

## 限制读取行数

工厂方法支持：

```java
ReadSheet sheet = EasyExcel.readSheet(0, "Sheet1", 1000).build();
```

`ReadListener.hasNext` 源码会读取 `ReadSheet.numRows` 或 `ReadWorkbook.numRows`，当前行号达到限制时停止。

## 多 sheet 读取

```java
ExcelReader reader = EasyExcel.read(inputStream).autoCloseStream(false).build();
try {
    ReadSheet sheet1 = EasyExcel.readSheet(0).head(UserImport.class).registerReadListener(userListener).build();
    ReadSheet sheet2 = EasyExcel.readSheet(1).head(RoleImport.class).registerReadListener(roleListener).build();
    reader.read(sheet1, sheet2);
} finally {
    reader.finish();
}
```

## 异常处理

转换异常通常是 `ExcelDataConvertException`：

```java
@Override
public void onException(Exception exception, AnalysisContext context) throws Exception {
    if (exception instanceof ExcelDataConvertException e) {
        Integer row = e.getRowIndex();
        Integer col = e.getColumnIndex();
        throw new ExcelAnalysisException("第" + (row + 1) + "行第" + (col + 1) + "列解析失败", e);
    }
    throw exception;
}
```

注意：

- rowIndex/columnIndex 通常从 0 开始，展示给用户时可 +1。
- `onException` 默认抛出会终止读取；如需跳过错误行，必须明确记录错误并决定是否继续。
- 业务校验错误和格式转换错误要区分。

## 导入安全与性能

- 限制上传文件大小、sheet 数、行数、列数。
- 大文件监听器分批入库，不要攒全部数据。
- 对每行做业务校验，失败信息要包含行号和列名。
- 不要信任 Excel 表头，必要时校验表头名称。
- 上传文件扩展名和内容类型只能作参考，不能替代解析限制。
