package com.jimuqu.generator.domain.vo;

import cn.xbatis.db.annotations.NestedResultEntity;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.generator.constant.GenConstants;
import com.jimuqu.generator.domain.GenTable;
import com.jimuqu.generator.domain.GenTableColumn;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.dromara.autotable.annotation.Ignore;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务表 gen_table
 *
 * @author Lion Li,chengliang4810
 */
@Data
@ResultEntity(GenTable.class)
@AutoMapper(target = GenTable.class)
public class GenTableVo implements Serializable {

    private Long id;

    /**
     * 数据源名称
     */
    private String dataName;

    /**
     * 表名称
     */
    private String tableName;

    /**
     * 表描述
     */
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
    private String className;

    /**
     * 使用的模板（crud单表操作 tree树表操作 sub主子表操作）
     */
    private String tplCategory;

    /**
     * 生成包路径
     */
    private String packageName;

    /**
     * 生成模块名
     */
    private String moduleName;

    /**
     * 生成业务名
     */
    private String businessName;

    /**
     * 生成功能名
     */
    private String functionName;

    /**
     * 生成作者
     */
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
    @NestedResultEntity(target = GenTableColumn.class)
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

    /**
     * 创建者
     */
    private Long createBy;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新者
     */
    private Long updateBy;
    /**
     * 更新时间
     */
    private Date updateTime;


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
