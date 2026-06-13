package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysPlugin;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.NotNull;

/**
 * 在线插件业务对象。
 *
 * @author jimuqu-admin
 * @since 2026-06-13
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysPlugin.class, reverseConvertGenerate = false)
public class SysPluginBo extends BoBaseEntity {

    /**
     * 插件主键。
     */
    @NotNull(message = "插件主键不能为空", groups = { UpdateGroup.class })
    private Long id;

    /**
     * 插件编码。
     */
    @NotBlank(message = "插件编码不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String pluginKey;

    /**
     * 插件名称。
     */
    @NotBlank(message = "插件名称不能为空", groups = { AddGroup.class, UpdateGroup.class })
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
}
