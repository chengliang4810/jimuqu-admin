package com.jimuqu.generator.util;

import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.generator.config.GenConfig;
import com.jimuqu.generator.constant.GenConstants;
import com.jimuqu.generator.domain.GenTableColumn;
import com.jimuqu.generator.domain.vo.GenTableVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RegExUtils;
import org.noear.solon.Solon;

import java.util.Arrays;
import java.util.List;

/**
 * 代码生成器 工具类
 *
 * @author ruoyi
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenUtils {

    private static GenConfig genConfig;

    static {
        Solon.context().getBeanAsync(GenConfig.class, (config) -> genConfig = config);
    }

    /**
     * 初始化表信息
     */
    public static void initTable(GenTableVo genTable, Long operId) {
        genTable.setClassName(convertClassName(genTable.getTableName()));
        genTable.setPackageName(genConfig.getPackageName());
        genTable.setModuleName(getModuleName(genTable.getPackageName()));
        genTable.setBusinessName(getBusinessName(genTable.getTableName()));
        genTable.setFunctionName(replaceText(genTable.getTableComment()));
        genTable.setFunctionAuthor(genConfig.getAuthor());
        genTable.setCreateBy(operId);
    }

    /**
     * 初始化列属性字段
     */
    public static void initColumnField(GenTableColumn column, GenTableVo table) {
        String dataType = getDbType(column.getColumnType());
        String columnName = column.getColumnName();
        column.setTableId(table.getId());
        column.setCreateBy(table.getCreateBy());
        // 设置java字段名
        column.setJavaField(StringUtil.toCamelCase(columnName));
        // 设置默认类型
        column.setJavaType(GenConstants.TYPE_STRING);
        column.setQueryType(GenConstants.QUERY_EQ);

        if (arraysContains(GenConstants.COLUMNTYPE_STR, dataType) || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType)) {
            // 字符串长度超过500设置为文本域
            Integer columnLength = getColumnLength(column.getColumnType());
            String htmlType = columnLength >= 500 || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType) ? GenConstants.HTML_TEXTAREA : GenConstants.HTML_INPUT;
            column.setHtmlType(htmlType);
        } else if (arraysContains(GenConstants.COLUMNTYPE_TIME, dataType)) {
            column.setJavaType(GenConstants.TYPE_DATE);
            column.setHtmlType(GenConstants.HTML_DATETIME);
            column.setLength(-1);
            column.setQueryType(GenConstants.QUERY_BETWEEN);
        } else if (arraysContains(GenConstants.COLUMNTYPE_NUMBER, dataType)) {
            column.setHtmlType(GenConstants.HTML_INPUT);
            column.setLength(-1);
            // 如果是浮点型 统一用BigDecimal
            List<String> str = StringUtil.splitList(StringUtil.subBetween(column.getColumnType(), "(", ")" ), StringUtil.SEPARATOR);
            if (str != null && str.size() == 2 && Integer.parseInt(str.get(1)) > 0) {
                column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
            }
            // 如果是整形
            else if (str != null && str.size() == 1 && Integer.parseInt(str.get(0)) <= 10) {
                column.setJavaType(GenConstants.TYPE_INTEGER);
            }
            // 长整形
            else {
                column.setJavaType(GenConstants.TYPE_LONG);
            }
        }

        // BO对象 默认插入勾选
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_ADD, columnName) && !column.isPk()) {
            column.setIsInsert(GenConstants.REQUIRE);
        }
        // BO对象 默认编辑勾选
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_EDIT, columnName)) {
            column.setIsEdit(GenConstants.REQUIRE);
        }
        // BO对象 默认是否必填勾选
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_EDIT, columnName)) {
            column.setIsRequired(GenConstants.REQUIRE);
        }
        // VO对象 默认返回勾选
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_LIST, columnName)) {
            column.setIsList(GenConstants.REQUIRE);
        }
        // BO对象 默认查询勾选
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_QUERY, columnName) && !column.isPk()) {
            column.setIsQuery(GenConstants.REQUIRE);
        }

        // 查询字段类型
        if (StringUtil.endWithAnyIgnoreCase(columnName, "name" )) {
            column.setQueryType(GenConstants.QUERY_LIKE);
        }
        // 状态字段设置单选框
        if (StringUtil.endWithAnyIgnoreCase(columnName, "status" )) {
            column.setHtmlType(GenConstants.HTML_RADIO);
        }
        // 类型&性别字段设置下拉框
        else if (StringUtil.endWithAnyIgnoreCase(columnName, "type" )
                || StringUtil.endWithAnyIgnoreCase(columnName, "sex" )) {
            column.setHtmlType(GenConstants.HTML_SELECT);
        }
        // 图片字段设置图片上传控件
        else if (StringUtil.endWithAnyIgnoreCase(columnName, "image" )) {
            column.setHtmlType(GenConstants.HTML_IMAGE_UPLOAD);
        }
        // 文件字段设置文件上传控件
        else if (StringUtil.endWithAnyIgnoreCase(columnName, "file" )) {
            column.setHtmlType(GenConstants.HTML_FILE_UPLOAD);
        }
        // 内容字段设置富文本控件
        else if (StringUtil.endWithAnyIgnoreCase(columnName, "content" )) {
            column.setHtmlType(GenConstants.HTML_EDITOR);
        }
    }

    /**
     * 校验数组是否包含指定值
     *
     * @param arr         数组
     * @param targetValue 值
     * @return 是否包含
     */
    public static boolean arraysContains(String[] arr, String targetValue) {
        return Arrays.asList(arr).contains(targetValue);
    }

    /**
     * 获取模块名
     *
     * @param packageName 包名
     * @return 模块名
     */
    public static String getModuleName(String packageName) {
        int lastIndex = packageName.lastIndexOf("." );
        int nameLength = packageName.length();
        return StringUtil.substring(packageName, lastIndex + 1, nameLength);
    }

    /**
     * 获取业务名
     *
     * @param tableName 表名
     * @return 业务名
     */
    public static String getBusinessName(String tableName) {
        int firstIndex = tableName.indexOf("_" );
        int nameLength = tableName.length();
        String businessName = StringUtil.substring(tableName, firstIndex + 1, nameLength);
        businessName = StringUtil.toCamelCase(businessName);
        return businessName;
    }

    /**
     * 表名转换成Java类名
     *
     * @param tableName 表名称
     * @return 类名
     */
    public static String convertClassName(String tableName) {
        boolean autoRemovePre = genConfig.getAutoRemovePre();
        String tablePrefix = genConfig.getTablePrefix();
        if (autoRemovePre && StringUtil.isNotEmpty(tablePrefix)) {
            List<String> searchList = StringUtil.splitList(tablePrefix, StringUtil.SEPARATOR);
            tableName = replaceFirst(tableName, searchList);
        }
        return StringUtil.convertToCamelCase(tableName);
    }

    /**
     * 批量替换前缀
     *
     * @param replacementm 替换值
     * @param searchList   替换列表
     */
    public static String replaceFirst(String replacementm, List<String> searchList) {
        String text = replacementm;
        for (String searchString : searchList) {
            if (replacementm.startsWith(searchString)) {
                text = replacementm.replaceFirst(searchString, StringUtil.EMPTY);
                break;
            }
        }
        return text;
    }

    /**
     * 关键字替换
     *
     * @param text 需要被替换的名字
     * @return 替换后的名字
     */
    public static String replaceText(String text) {
        return RegExUtils.replaceAll(text, "(?:表|若依)", "" );
    }

    /**
     * 获取数据库类型字段
     *
     * @param columnType 列类型
     * @return 截取后的列类型
     */
    public static String getDbType(String columnType) {
        if (StringUtil.indexOf(columnType, '(' ) > 0) {
            return StringUtil.subBefore(columnType, "(", false);
        } else {
            return columnType;
        }
    }

    /**
     * 获取字段长度
     *
     * @param columnType 列类型
     * @return 截取后的列类型
     */
    public static Integer getColumnLength(String columnType) {
        if (StringUtil.indexOf(columnType, '(' ) > 0) {
            String length = StringUtil.subBetween(columnType, "(", ")" );
            return Integer.valueOf(length);
        } else {
            return 0;
        }
    }
}
