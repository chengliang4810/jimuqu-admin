package com.jimuqu.common.excel.core;

import cn.idev.excel.metadata.FieldCache;
import cn.idev.excel.metadata.FieldWrapper;
import cn.idev.excel.util.ClassUtils;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.service.DictService;
import com.jimuqu.common.core.utils.StreamUtil;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.annotation.ExcelDynamicOptions;
import com.jimuqu.common.excel.annotation.ExcelEnumFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import org.noear.solon.Solon;

import java.lang.reflect.Field;
import java.util.*;

/**
 * <h1>Excel表格下拉选操作</h1>
 * 考虑到下拉选过多可能导致Excel打开缓慢的问题，只校验前1000行
 * <p>
 * 即只有前1000行的数据可以用下拉框，超出的自行通过限制数据量的形式，第二次输出
 *
 * @author Emil.Zhang
 */
@Slf4j
public class ExcelDownHandler implements SheetWriteHandler {

    private static final int FIRST_DATA_ROW_INDEX = 1;
    private static final int LAST_DATA_ROW_INDEX = 1000;
    /**
     * 单选数据Sheet名
     */
    private static final String OPTIONS_SHEET_NAME = "options";
    /**
     * 联动选择数据Sheet名的头
     */
    private static final String LINKED_OPTIONS_SHEET_NAME = "linkedOptions";
    /**
     * 下拉可选项
     */
    private final List<DropDownOptions> dropDownOptions;
    /**
     * 当前单选进度
     */
    private int currentOptionsColumnIndex;
    /**
     * 当前联动选择进度
     */
    private int currentLinkedOptionsSheetIndex;
    private final DictService dictService;

    public ExcelDownHandler(List<DropDownOptions> options) {
        this.dropDownOptions = options;
        this.currentOptionsColumnIndex = 0;
        this.currentLinkedOptionsSheetIndex = 0;
        this.dictService = Solon.context() == null ? null : Solon.context().getBean(DictService.class);
    }

    /**
     * <h2>开始创建下拉数据</h2>
     * 1.通过解析传入的@ExcelProperty同级是否标注有@DropDown选项
     * 如果有且设置了value值，则将其直接置为下拉可选项
     * <p>
     * 2.或者在调用ExcelUtil时指定了可选项，将依据传入的可选项做下拉
     * <p>
     * 3.二者并存，注意调用方式
     */
    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        // 开始设置下拉框 HSSFWorkbook
        DataValidationHelper helper = sheet.getDataValidationHelper();
        Workbook workbook = writeWorkbookHolder.getWorkbook();
        FieldCache fieldCache = ClassUtils.declaredFields(writeWorkbookHolder.getClazz(), writeWorkbookHolder);
        for (Map.Entry<Integer, FieldWrapper> entry : fieldCache.getSortedFieldMap().entrySet()) {
            Integer index = entry.getKey();
            FieldWrapper wrapper = entry.getValue();
            Field field = wrapper.getField();
            // 循环实体中的每个属性
            // 可选的下拉值
            List<String> options = new ArrayList<>();
            if (field.isAnnotationPresent(ExcelDictFormat.class)) {
                // 如果指定了@ExcelDictFormat，则使用字典的逻辑
                ExcelDictFormat format = field.getDeclaredAnnotation(ExcelDictFormat.class);
                String dictType = format.dictType();
                String converterExp = format.readConverterExp();
                if (StrUtil.isNotBlank(dictType)) {
                    // 如果传递了字典名，则依据字典建立下拉
                    if (dictService == null) {
                        throw new ServiceException("未初始化字典服务，无法创建字典下拉：" + dictType);
                    }
                    Collection<String> values = Optional.ofNullable(dictService.getAllDictByDictType(dictType))
                            .orElseThrow(() -> new ServiceException(String.format("字典 %s 不存在", dictType)))
                            .values();
                    options = new ArrayList<>(values);
                } else if (StrUtil.isNotBlank(converterExp)) {
                    // 如果指定了确切的值，则直接解析确切的值
                    options = StreamUtil.toList(
                            StrUtil.split(converterExp, ",", true, true),
                            expression -> {
                                String[] parts = expression.split("=", 2);
                                if (parts.length != 2) {
                                    throw new ServiceException("Excel转换表达式格式错误：" + expression);
                                }
                                return parts[1];
                            });
                }
            } else if (field.isAnnotationPresent(ExcelEnumFormat.class)) {
                // 否则如果指定了@ExcelEnumFormat，则使用枚举的逻辑
                ExcelEnumFormat format = field.getDeclaredAnnotation(ExcelEnumFormat.class);
                List<Object> values = EnumUtil.getFieldValues((Class) format.enumClass(), format.textField());
                options = StreamUtil.toList(values, String::valueOf);
            } else if (field.isAnnotationPresent(ExcelDynamicOptions.class)) {
                ExcelDynamicOptions dynamicOptions = field.getDeclaredAnnotation(ExcelDynamicOptions.class);
                ExcelOptionsProvider provider = Solon.context().getBean(dynamicOptions.providerClass());
                if (provider == null) {
                    throw new ServiceException("未初始化 Excel 动态下拉选项提供器："
                            + dynamicOptions.providerClass().getName());
                }
                Set<String> providerOptions = provider.getOptions();
                if (CollUtil.isNotEmpty(providerOptions)) {
                    options = new ArrayList<>(providerOptions);
                }
            }
            if (ObjectUtil.isNotEmpty(options)) {
                // 仅当下拉可选项不为空时执行
                int totalCharacters = options.stream().mapToInt(String::length).sum() + options.size();
                if (options.size() > 20 || totalCharacters > 255) {
                    // Excel 的显式列表公式最长 255 个字符，超出时使用隐藏表。
                    dropDownWithSheet(helper, workbook, sheet, index, options);
                } else {
                    // 否则使用固定值形式
                    dropDownWithSimple(helper, sheet, index, options);
                }
            }
        }
        if (CollUtil.isEmpty(dropDownOptions)) {
            return;
        }
        dropDownOptions.forEach(everyOptions -> {
            // 如果传递了下拉框选择器参数
            if (CollUtil.isNotEmpty(everyOptions.getNextOptions())) {
                // 当二级选项不为空时，使用额外关联表的形式
                dropDownLinkedOptions(helper, workbook, sheet, everyOptions);
            } else if (CollUtil.isNotEmpty(everyOptions.getOptions()) && everyOptions.getOptions().size() > 10) {
                // 当一级选项参数个数大于10，使用额外表的形式
                dropDownWithSheet(helper, workbook, sheet, everyOptions.getIndex(), everyOptions.getOptions());
            } else if (CollUtil.isNotEmpty(everyOptions.getOptions())) {
                // 当一级选项个数不为空，使用默认形式
                dropDownWithSimple(helper, sheet, everyOptions.getIndex(), everyOptions.getOptions());
            }
        });
    }

    /**
     * <h2>简单下拉框</h2>
     * 直接将可选项拼接为指定列的数据校验值
     *
     * @param celIndex 列index
     * @param value    下拉选可选值
     */
    private void dropDownWithSimple(DataValidationHelper helper, Sheet sheet, Integer celIndex, List<String> value) {
        if (ObjectUtil.isEmpty(value)) {
            return;
        }
        this.markOptionsToSheet(helper, sheet, celIndex, helper.createExplicitListConstraint(value.toArray(String[]::new)));
    }

    /**
     * <h2>额外表格形式的级联下拉框</h2>
     *
     * @param options 额外表格形式存储的下拉可选项
     */
    private void dropDownLinkedOptions(DataValidationHelper helper, Workbook workbook, Sheet sheet, DropDownOptions options) {
        String linkedOptionsSheetName = String.format("%s_%d", LINKED_OPTIONS_SHEET_NAME, currentLinkedOptionsSheetIndex);
        // 创建联动下拉数据表
        Sheet linkedOptionsDataSheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(linkedOptionsSheetName));
        // 将下拉表隐藏
        workbook.setSheetHidden(workbook.getSheetIndex(linkedOptionsDataSheet), true);
        // 完善横向的一级选项数据表
        List<String> firstOptions = options.getOptions();
        if (CollUtil.isEmpty(firstOptions)) {
            return;
        }
        validateLinkedOptionNames(firstOptions);
        Map<String, List<String>> secoundOptionsMap = options.getNextOptions();

        // 创建名称管理器
        Name name = workbook.createName();
        // 设置名称管理器的别名
        name.setNameName(linkedOptionsSheetName);
        // 以横向第一行创建一级下拉拼接引用位置
        String firstOptionsFunction = String.format("%s!$%s$1:$%s$1",
                linkedOptionsSheetName,
                getExcelColumnName(0),
                getExcelColumnName(firstOptions.size() - 1)
        );
        // 设置名称管理器的引用位置
        name.setRefersToFormula(firstOptionsFunction);
        // 设置数据校验为序列模式，引用的是名称管理器中的别名
        this.markOptionsToSheet(helper, sheet, options.getIndex(), helper.createFormulaListConstraint(linkedOptionsSheetName));

        for (int columIndex = 0; columIndex < firstOptions.size(); columIndex++) {
            // 先提取主表中一级下拉的列名
            String firstOptionsColumnName = getExcelColumnName(columIndex);
            // 一次循环是每一个一级选项
            int finalI = columIndex;
            // 本次循环的一级选项值
            String thisFirstOptionsValue = firstOptions.get(columIndex);
            // 创建第一行的数据
            Optional.ofNullable(linkedOptionsDataSheet.getRow(0))
                    // 如果不存在则创建第一行
                    .orElseGet(() -> linkedOptionsDataSheet.createRow(finalI))
                    // 第一行当前列
                    .createCell(columIndex)
                    // 设置值为当前一级选项值
                    .setCellValue(thisFirstOptionsValue);

            // 第二行开始，设置第二级别选项参数
            List<String> secondOptions = secoundOptionsMap.get(thisFirstOptionsValue);
            if (CollUtil.isEmpty(secondOptions)) {
                // 必须保证至少有一个关联选项，否则将导致Excel解析错误
                secondOptions = Collections.singletonList("暂无_0" );
            }

            // 以该一级选项值创建子名称管理器
            Name sonName = workbook.createName();
            // 设置名称管理器的别名
            sonName.setNameName(thisFirstOptionsValue);
            // 以第二行该列数据拼接引用位置
            String sonFunction = String.format("%s!$%s$2:$%s$%d",
                    linkedOptionsSheetName,
                    firstOptionsColumnName,
                    firstOptionsColumnName,
                    secondOptions.size() + 1
            );
            // 设置名称管理器的引用位置
            sonName.setRefersToFormula(sonFunction);
            // 数据验证为序列模式，引用到每一个主表中的二级选项位置
            // 创建子项的名称管理器，只是为了使得Excel可以识别到数据
            String mainSheetFirstOptionsColumnName = getExcelColumnName(options.getIndex());
            for (int i = FIRST_DATA_ROW_INDEX; i <= LAST_DATA_ROW_INDEX; i++) {
                // 以一级选项对应的主体所在位置创建二级下拉
                String secondOptionsFunction = String.format("=INDIRECT(%s%d)", mainSheetFirstOptionsColumnName, i + 1);
                // 二级只能主表每一行的每一列添加二级校验
                markLinkedOptionsToSheet(helper, sheet, i, options.getNextIndex(), helper.createFormulaListConstraint(secondOptionsFunction));
            }

            for (int rowIndex = 0; rowIndex < secondOptions.size(); rowIndex++) {
                // 从第二行开始填充二级选项
                int finalRowIndex = rowIndex + 1;
                int finalColumIndex = columIndex;

                Row row = Optional.ofNullable(linkedOptionsDataSheet.getRow(finalRowIndex))
                        // 没有则创建
                        .orElseGet(() -> linkedOptionsDataSheet.createRow(finalRowIndex));
                Optional
                        // 在本级一级选项所在的列
                        .ofNullable(row.getCell(finalColumIndex))
                        // 不存在则创建
                        .orElseGet(() -> row.createCell(finalColumIndex))
                        // 设置二级选项值
                        .setCellValue(secondOptions.get(rowIndex));
            }
        }

        currentLinkedOptionsSheetIndex++;
    }

    private void validateLinkedOptionNames(List<String> firstOptions) {
        Set<String> names = new HashSet<>();
        for (String firstOption : firstOptions) {
            DropDownOptions.validateOptionValue(firstOption);
            if (!names.add(firstOption)) {
                throw new ServiceException("级联下拉一级选项重复：" + firstOption);
            }
        }
    }

    /**
     * <h2>额外表格形式的普通下拉框</h2>
     * 由于下拉框可选值数量过多，为提升Excel打开效率，使用额外表格形式做下拉
     *
     * @param celIndex 下拉选
     * @param value    下拉选可选值
     */
    private void dropDownWithSheet(DataValidationHelper helper, Workbook workbook, Sheet sheet, Integer celIndex, List<String> value) {
        // 创建下拉数据表
        Sheet simpleDataSheet = Optional.ofNullable(workbook.getSheet(WorkbookUtil.createSafeSheetName(OPTIONS_SHEET_NAME)))
                .orElseGet(() -> workbook.createSheet(WorkbookUtil.createSafeSheetName(OPTIONS_SHEET_NAME)));
        // 将下拉表隐藏
        workbook.setSheetHidden(workbook.getSheetIndex(simpleDataSheet), true);
        // 完善纵向的一级选项数据表
        for (int i = 0; i < value.size(); i++) {
            int finalI = i;
            // 获取每一选项行，如果没有则创建
            Row row = Optional.ofNullable(simpleDataSheet.getRow(i))
                    .orElseGet(() -> simpleDataSheet.createRow(finalI));
            // 获取本级选项对应的选项列，如果没有则创建
            Cell cell = Optional.ofNullable(row.getCell(currentOptionsColumnIndex))
                    .orElseGet(() -> row.createCell(currentOptionsColumnIndex));
            // 设置值
            cell.setCellValue(value.get(i));
        }

        // 创建名称管理器
        Name name = workbook.createName();
        // 设置名称管理器的别名
        String nameName = String.format("%s_%d", OPTIONS_SHEET_NAME, celIndex);
        name.setNameName(nameName);
        // 以纵向第一列创建一级下拉拼接引用位置
        String function = String.format("%s!$%s$1:$%s$%d",
                OPTIONS_SHEET_NAME,
                getExcelColumnName(currentOptionsColumnIndex),
                getExcelColumnName(currentOptionsColumnIndex),
                value.size());
        // 设置名称管理器的引用位置
        name.setRefersToFormula(function);
        // 设置数据校验为序列模式，引用的是名称管理器中的别名
        this.markOptionsToSheet(helper, sheet, celIndex, helper.createFormulaListConstraint(nameName));
        currentOptionsColumnIndex++;
    }

    /**
     * 挂载下拉的列，仅限一级选项
     */
    private void markOptionsToSheet(DataValidationHelper helper, Sheet sheet, Integer celIndex,
                                    DataValidationConstraint constraint) {
        // 设置数据有效性加载在哪个单元格上,四个参数分别是：起始行、终止行、起始列、终止列
        CellRangeAddressList addressList = new CellRangeAddressList(
                FIRST_DATA_ROW_INDEX, LAST_DATA_ROW_INDEX, celIndex, celIndex);
        markDataValidationToSheet(helper, sheet, constraint, addressList);
    }

    /**
     * 挂载下拉的列，仅限二级选项
     */
    private void markLinkedOptionsToSheet(DataValidationHelper helper, Sheet sheet, Integer rowIndex,
                                          Integer celIndex, DataValidationConstraint constraint) {
        // 设置数据有效性加载在哪个单元格上,四个参数分别是：起始行、终止行、起始列、终止列
        CellRangeAddressList addressList = new CellRangeAddressList(rowIndex, rowIndex, celIndex, celIndex);
        markDataValidationToSheet(helper, sheet, constraint, addressList);
    }

    /**
     * 应用数据校验
     */
    private void markDataValidationToSheet(DataValidationHelper helper, Sheet sheet,
                                           DataValidationConstraint constraint, CellRangeAddressList addressList) {
        // 数据有效性对象
        DataValidation dataValidation = helper.createValidation(constraint, addressList);
        // 处理Excel兼容性问题
        if (dataValidation instanceof XSSFDataValidation) {
            //数据校验
            dataValidation.setSuppressDropDownArrow(true);
            //错误提示
            dataValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            dataValidation.createErrorBox("提示", "此值与单元格定义数据不一致" );
            dataValidation.setShowErrorBox(true);
            //选定提示
            dataValidation.createPromptBox("填写说明：", "填写内容只能为下拉中数据，其他数据将导致导入失败" );
            dataValidation.setShowPromptBox(true);
        } else {
            dataValidation.setSuppressDropDownArrow(false);
        }
        sheet.addValidationData(dataValidation);
    }

    /**
     * <h2>依据列index获取列名英文</h2>
     * 依据列index转换为Excel中的列名英文
     * <p>例如第1列，index为0，解析出来为A列</p>
     * 第27列，index为26，解析为AA列
     * <p>第28列，index为27，解析为AB列</p>
     *
     * @param columnIndex 列index
     * @return 列index所在得英文名
     */
    private String getExcelColumnName(int columnIndex) {
        return CellReference.convertNumToColString(columnIndex);
    }
}
