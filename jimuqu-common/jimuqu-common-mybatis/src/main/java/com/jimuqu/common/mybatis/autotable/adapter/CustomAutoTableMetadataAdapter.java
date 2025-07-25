package com.jimuqu.common.mybatis.autotable.adapter;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Ignore;
import cn.xbatis.db.annotations.Ignores;
import cn.xbatis.db.annotations.TableId;
import com.github.therapi.runtimejavadoc.*;
import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.AutoTableMetadataAdapter;
import org.dromara.autotable.solon.util.AnnotatedElementUtilsPlus;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.map.MapUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 自定义自动表元数据适配器
 *
 * @author chengliang
 * @since 2025/05/21
 */
public class CustomAutoTableMetadataAdapter implements AutoTableMetadataAdapter {

    /**
     * 注释格式化器
     */
    private static final CommentFormatter formatter = new CommentFormatter();
    /**
     * 列类型标签
     */
    private static final String COLUMN_TYPE_TAG = "type";
    /**
     * 列长度标签
     */
    private static final String COLUMN_LENGTH_TAG = "length";
    /**
     * 列小数长度标签
     */
    private static final String COLUMN_DECIMAL_LENGTH_TAG = "decimalLength";
    /**
     * 列默认值标签
     */
    private static final String COLUMN_DEFAULT_VALUE_TAG = "default";

    /**
     * 忽略字段
     *
     * @param field 场
     * @param clazz clazz
     * @return boolean
     */
    @Override
    public Boolean isIgnoreField(Field field, Class<?> clazz) {
        Ignore ignore = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, Ignore.class);
        if (ignore != null) {
            return true;
        }

        // 通过Ignores判断是否忽略
        Ignores ignoresAnnotation = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(clazz, Ignores.class);
        if (ignoresAnnotation != null) {
            return Arrays.stream(ignoresAnnotation.value()).anyMatch(property -> property.equals(field.getName()));
        }
        return false;
    }

    /**
     * 主键
     *
     * @param field 字段
     * @param clazz 类型
     * @return boolean
     */
    @Override
    public Boolean isPrimary(Field field, Class<?> clazz) {
        // 通过TableId判断是否为主键
        if (AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, TableId.class) != null) {
            return true;
        }
        // 字段名如果为 ID 则认为是主键
        return "id".equals(field.getName());
    }

    /**
     * 自动递增
     *
     * @param field 字段
     * @param clazz 类型
     * @return boolean
     */
    @Override
    public Boolean isAutoIncrement(Field field, Class<?> clazz) {

        if (!isPrimary(field, clazz)) {
            return false;
        }

        TableId tableId = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, TableId.class);
        return tableId != null && tableId.value() == IdAutoType.AUTO;
    }

    /**
     * 表注释
     *
     * @param clazz 实体类
     * @return
     */
    @Override
    public String getTableComment(Class<?> clazz) {
        ClassJavadoc classDoc = RuntimeJavadoc.getJavadoc(clazz);
        if (classDoc != null && !classDoc.isEmpty()) {
            return formatter.format(classDoc.getComment());
        }
        return null;
    }

    /**
     * 字段注释
     *
     * @param field 字段
     * @param clazz 实体类
     * @return 字段注释
     */
    @Override
    public String getColumnComment(Field field, Class<?> clazz) {
        FieldJavadoc fieldJavadoc = RuntimeJavadoc.getJavadoc(field);
        if (fieldJavadoc != null && !fieldJavadoc.isEmpty()) {
            return formatter.format(fieldJavadoc.getComment());
        }
        return null;
    }

    @Override
    public ColumnDefault getColumnDefaultValue(Field field, Class<?> clazz) {
        FieldJavadoc fieldJavadoc = RuntimeJavadoc.getJavadoc(field);
        if (fieldJavadoc != null && !fieldJavadoc.isEmpty()) {
            Map<String, String> columnTypeMap = parseColumnTypeMap(fieldJavadoc);
            String value = Optional.ofNullable(columnTypeMap.get(COLUMN_DEFAULT_VALUE_TAG)).orElse("");
            return new ColumnDefault() {
                @Override
                public DefaultValueEnum type() {
                    return DefaultValueEnum.UNDEFINED;
                }

                @Override
                public String value() {
                    return value;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return ColumnDefault.class;
                }
            };
        }
        return null;
    }

    @Override
    public ColumnType getColumnType(Field field, Class<?> clazz) {
        FieldJavadoc fieldJavadoc = RuntimeJavadoc.getJavadoc(field);
        if (fieldJavadoc != null && !fieldJavadoc.isEmpty()) {
            Map<String, String> columnTypeMap = parseColumnTypeMap(fieldJavadoc);
            if (CollUtil.isEmpty(columnTypeMap)) {
                return null;
            }

            return new ColumnType(){
                @Override
                public Class<? extends Annotation> annotationType() {
                    return ColumnType.class;
                }

                @Override
                public String value() {
                    return Optional.ofNullable(columnTypeMap.get(COLUMN_TYPE_TAG)).orElse("");
                }

                @Override
                public int length() {
                    return Optional.ofNullable(columnTypeMap.get(COLUMN_LENGTH_TAG)).map(Integer::parseInt).orElse(-1);
                }

                @Override
                public int decimalLength() {
                    return Optional.ofNullable(columnTypeMap.get(COLUMN_DECIMAL_LENGTH_TAG)).map(Integer::parseInt).orElse(-1);
                }

                @Override
                public String[] values() {
                    return new String[]{};
                }
            };
        }

        return null;
    }

    /**
     * 解析列类型映射
     *
     * @param fieldJavadoc 字段文档
     * @return {@link Map}<{@link String}, {@link String}>
     * 列类型映射
     */
    private Map<String, String> parseColumnTypeMap(FieldJavadoc fieldJavadoc) {
        List<OtherJavadoc> otherTagList = fieldJavadoc.getOther();
        if (CollUtil.isEmpty(otherTagList)) {
            return MapUtil.empty();
        }

        return otherTagList.stream().filter(otherJavadoc -> otherJavadoc.getComment() != null)
                .collect(Collectors.toMap(OtherJavadoc::getName, otherJavadoc -> otherJavadoc.getComment().toString()));
    }

}
