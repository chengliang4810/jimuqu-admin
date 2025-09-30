package com.jimuqu.generator.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

import java.io.Serial;

/**
 * 代码生成模板对象 gen_template
 *
 * @author chengliang4810
 * @date 2025-01-05
 */
@Data
@FieldNameConstants
@Table("gen_template")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoTable(value = "gen_template", comment = "代码模板")
public class GenTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = IdAutoType.GENERATOR, generatorName = IdentifierGeneratorType.DEFAULT)
    protected Long id;

    /**
     * 模板分类，用于批量选择模板
     */
    @AutoColumn(comment = "模板分类，用于批量选择模板")
    private String category;

    /**
     * 模板名称
     */
    @AutoColumn(comment = "模板名称")
    private String name;

    /**
     * 文件路径
     */
    @AutoColumn(comment = "文件路径")
    private String path;

    /**
     * 数据库类型
     */
    @AutoColumn(comment = "数据库类型")
    private String dbType;

    /**
     * 模板内容
     */
    @ColumnType(MysqlTypeConstant.TEXT)
    private String content;

    /**
     * 是否启用
     */
    @AutoColumn(comment = "是否启用", defaultValue = "1")
    private Integer enable;

    /**
     * 备注
     */
    @AutoColumn(comment = "备注")
    private String remark;


}
