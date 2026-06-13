package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

import java.io.Serial;

/**
 * 在线插件。
 *
 * @author jimuqu-admin
 * @since 2026-06-13
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_plugin")
public class SysPlugin extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 插件主键。
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "插件主键")
    private Long id;

    /**
     * 插件编码。
     */
    @AutoColumn(comment = "插件编码", length = 100)
    private String pluginKey;

    /**
     * 插件名称。
     */
    @AutoColumn(comment = "插件名称", length = 100)
    private String pluginName;

    /**
     * 插件版本。
     */
    @AutoColumn(comment = "插件版本", length = 50)
    private String version;

    /**
     * 作者。
     */
    @AutoColumn(comment = "作者", length = 100)
    private String author;

    /**
     * 插件类型。
     */
    @AutoColumn(comment = "插件类型", length = 50)
    private String pluginType;

    /**
     * 插件入口。
     */
    @AutoColumn(comment = "插件入口", length = 255)
    private String entryClass;

    /**
     * 状态（0启用 1停用）。
     */
    @AutoColumn(comment = "状态（0启用 1停用）", defaultValue = "1")
    private Integer status;

    /**
     * 插件包路径。
     */
    @AutoColumn(comment = "插件包路径", length = 1000)
    private String packagePath;

    /**
     * 描述文件路径。
     */
    @AutoColumn(comment = "描述文件路径", length = 1000)
    private String descriptorPath;

    /**
     * 插件描述。
     */
    @AutoColumn(comment = "插件描述", length = 500)
    private String description;

    /**
     * 描述文件内容。
     */
    @AutoColumn(comment = "描述文件内容", type = MysqlTypeConstant.TEXT)
    private String manifestJson;
}
