package com.jimuqu.common.excel.convert;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import com.jimuqu.common.core.utils.reflect.ReflectUtil;
import com.jimuqu.common.excel.annotation.ExcelEnumFormat;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.annotation.AnnotationUtil;
import cn.hutool.v7.core.convert.ConvertUtil;
import cn.hutool.v7.core.util.ObjUtil;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 枚举格式化转换处理
 *
 * @author Liang
 */
@Slf4j
public class ExcelEnumConvert implements Converter<Object> {

    @Override
    public Class<Object> supportJavaTypeKey() {
        return Object.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return null;
    }

    @Override
    public Object convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        cellData.checkEmpty();
        // Excel中填入的是枚举中指定的描述
        Object textValue = switch (cellData.getType()) {
            case STRING, DIRECT_STRING, RICH_TEXT_STRING -> cellData.getStringValue();
            case NUMBER -> cellData.getNumberValue();
            case BOOLEAN -> cellData.getBooleanValue();
            default -> throw new IllegalArgumentException("单元格类型异常!" );
        };
        // 如果是空值
        if (ObjUtil.isNull(textValue)) {
            return null;
        }
        Map<Object, String> enumCodeToTextMap = beforeConvert(contentProperty);
        // 从Java输出至Excel是code转text
        // 因此从Excel转Java应该将text与code对调
        Map<String, Object> enumTextToCodeMap = new HashMap<>();
        enumCodeToTextMap.forEach((key, value) -> {
            Object previous = enumTextToCodeMap.put(value, key);
            if (previous != null) {
                throw new IllegalArgumentException("枚举导入文本值重复: " + value);
            }
        });
        // 应该从text -> code中查找
        Object codeValue = enumTextToCodeMap.get(ConvertUtil.toStr(textValue));
        if (ObjUtil.isNull(codeValue)) {
            throw new IllegalArgumentException("枚举值不匹配: " + textValue + "，允许值: " + enumTextToCodeMap.keySet());
        }
        return ConvertUtil.convert(contentProperty.getField().getType(), codeValue);
    }

    @Override
    public WriteCellData<String> convertToExcelData(Object object, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (ObjUtil.isNull(object)) {
            return new WriteCellData<>("" );
        }
        Map<Object, String> enumValueMap = beforeConvert(contentProperty);
        String value = ConvertUtil.toStr(enumValueMap.get(object), "" );
        return new WriteCellData<>(value);
    }

    private Map<Object, String> beforeConvert(ExcelContentProperty contentProperty) {
        ExcelEnumFormat anno = getAnnotation(contentProperty.getField());
        Map<Object, String> enumValueMap = new HashMap<>();
        Enum<?>[] enumConstants = anno.enumClass().getEnumConstants();
        for (Enum<?> enumConstant : enumConstants) {
            Object codeValue = ReflectUtil.invokeGetter(enumConstant, anno.codeField());
            String textValue = ReflectUtil.invokeGetter(enumConstant, anno.textField());
            if (ObjUtil.isNull(codeValue) || ObjUtil.isNull(textValue)) {
                throw new IllegalArgumentException("枚举字段 code/text 不能为空: " + enumConstant.name());
            }
            enumValueMap.put(codeValue, textValue);
        }
        return enumValueMap;
    }

    private ExcelEnumFormat getAnnotation(Field field) {
        return AnnotationUtil.getAnnotation(field, ExcelEnumFormat.class);
    }
}
