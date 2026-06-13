package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.system.domain.SysPlugin;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 在线插件视图对象。
 *
 * @author jimuqu-admin
 * @since 2026-06-13
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysPlugin.class)
@AutoMapper(target = SysPlugin.class)
public class SysPluginVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 插件主键。
     */
    private Long id;

    /**
     * 插件编码。
     */
    private String pluginKey;

    /**
     * 插件名称。
     */
    private String pluginName;

    /**
     * 插件版本。
     */
    private String version;

    /**
     * 作者。
     */
    private String author;

    /**
     * 插件类型。
     */
    private String pluginType;

    /**
     * 插件入口。
     */
    private String entryClass;

    /**
     * 状态（0启用 1停用）。
     */
    private Integer status;

    /**
     * 插件包路径。
     */
    private String packagePath;

    /**
     * 描述文件路径。
     */
    private String descriptorPath;

    /**
     * 插件描述。
     */
    private String description;

    /**
     * 描述文件内容。
     */
    private String manifestJson;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;
}
