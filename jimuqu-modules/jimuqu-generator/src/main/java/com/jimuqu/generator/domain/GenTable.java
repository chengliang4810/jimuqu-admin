package com.jimuqu.generator.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import com.jimuqu.generator.constant.GenConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.dromara.autotable.annotation.Ignore;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务表 gen_table
 *
 * @author Lion Li,chengliang4810
 */
@Data
@Table("gen_table")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class GenTable extends BaseEntity {

    /**
     * 主键
     */
    @TableId(value = IdAutoType.GENERATOR, generatorName = IdentifierGeneratorType.DEFAULT)
    protected Long id;

    /**
     * 数据源名称
     */
//    @NotBlank(message = "数据源名称不能为空")
    private String dataName;

    /**
     * 表名称
     */
    @NotBlank(message = "表名称不能为空")
    private String tableName;

    /**
     * 表描述
     */
    @NotBlank(message = "表描述不能为空")
    private String tableComment;

    /**
     * 关联父表的表名
     */
    private String subTableName;

    /**
     * 本表关联父表的外键名
     */
    private String subTableFkName;

    /**
     * 实体类名称(首字母大写)
     */
    @NotBlank(message = "实体类名称不能为空")
    private String className;

    /**
     * 使用的模板（crud单表操作 tree树表操作 sub主子表操作）
     */
    private String tplCategory;

    /**
     * 生成包路径
     */
    @NotBlank(message = "生成包路径不能为空")
    private String packageName;

    /**
     * 生成模块名
     */
    @NotBlank(message = "生成模块名不能为空")
    private String moduleName;

    /**
     * 生成业务名
     */
    @NotBlank(message = "生成业务名不能为空")
    private String businessName;

    /**
     * 生成功能名
     */
    @NotBlank(message = "生成功能名不能为空")
    private String functionName;

    /**
     * 生成作者
     */
    @NotBlank(message = "作者不能为空")
    private String functionAuthor;

    /**
     * 生成代码方式（0zip压缩包 1自定义路径）
     */
    private String genType;

    /**
     * 生成路径（不填默认项目路径）
     */
    private String genPath;

    /**
     * 主键信息
     */
    @cn.xbatis.db.annotations.Ignore
    private GenTableColumn pkColumn;

    /**
     * 表列信息
     */
    @Validated
    @cn.xbatis.db.annotations.Ignore
    // TODO
//    @RelationOneToMany(selfField = "tableId", targetField = "tableId", orderBy = "sort" )
    private List<GenTableColumn> columns;

    /**
     * 其它生成选项
     */
    private String options;

    /**
     * 备注
     */
    private String remark;

    /**
     * 树编码字段
     */
    @cn.xbatis.db.annotations.Ignore
    private String treeCode;

    /**
     * 树父编码字段
     */
    @cn.xbatis.db.annotations.Ignore
    private String treeParentCode;

    /**
     * 树名称字段
     */
    @cn.xbatis.db.annotations.Ignore
    private String treeName;

    /*
     * 菜单id列表
     */
    @cn.xbatis.db.annotations.Ignore
    private List<Long> menuIds;

    /**
     * 上级菜单ID字段
     */
    @cn.xbatis.db.annotations.Ignore
    private Long parentMenuId;

    /**
     * 上级菜单名称字段
     */
    @cn.xbatis.db.annotations.Ignore
    private String parentMenuName;

    @Ignore
    @cn.xbatis.db.annotations.Ignore
    private Map<String, Object> params = new HashMap<>();

    public boolean isTree() {
        return isTree(this.tplCategory);
    }

    public static boolean isTree(String tplCategory) {
        return tplCategory != null && StringUtils.equals(GenConstants.TPL_TREE, tplCategory);
    }

    public boolean isCrud() {
        return isCrud(this.tplCategory);
    }

    public static boolean isCrud(String tplCategory) {
        return tplCategory != null && StringUtils.equals(GenConstants.TPL_CRUD, tplCategory);
    }

    public boolean isSuperColumn(String javaField) {
        return isSuperColumn(this.tplCategory, javaField);
    }

    public static boolean isSuperColumn(String tplCategory, String javaField) {
        return StringUtils.equalsAnyIgnoreCase(javaField, GenConstants.BASE_ENTITY);
    }

}
