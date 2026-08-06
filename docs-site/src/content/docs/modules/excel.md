---
title: Excel 导入导出
description: FastExcel、字典转换、动态下拉与大数据量导出
---

`jimuqu-common-excel` 基于 FastExcel，提供通用监听器、字段转换、合并单元格、动态下拉选项和分页大数据量导出。

## 常用入口

- `ExcelUtil`：普通导入导出。
- `DefaultExcelListener`：将行数据转换并执行业务回调。
- `@ExcelDictFormat`：按系统字典转换显示值。
- `@ExcelEnumFormat`：按枚举转换。
- `@ExcelDynamicOptions`：生成动态下拉选项。
- `LargeExcelExportUtil`：分页拉取并导出大数据量。

## 导入原则

1. 先做模板字段和文件大小校验。
2. 逐行验证业务数据，返回可定位的失败行信息。
3. 批量写入并限制单批大小。
4. 明确是全部回滚还是允许部分成功。

用户管理已提供导入数据、导入模板和导出接口，可作为实际用法参考。
