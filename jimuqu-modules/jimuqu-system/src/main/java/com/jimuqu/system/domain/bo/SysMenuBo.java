package com.jimuqu.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jimuqu.common.core.constant.RegexConstants;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysMenu;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Pattern;
import org.noear.snack4.annotation.ONodeAttr;

/**
 * 菜单权限业务对象 menu
 *
 * @author chengliang4810
 * @since 2025-06-06
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysMenu.class, reverseConvertGenerate = false)
public class SysMenuBo extends BoBaseEntity {

    /**
     * 菜单ID
     */
    @NotNull(message = "菜单ID不能为空", groups = { UpdateGroup.class })
    @JsonProperty("menuId")
    @ONodeAttr(name = "menuId")
    private Long id;
    /**
     * 父菜单ID
     */
    @NotNull(message = "父菜单ID不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Long parentId;
    /**
     * 菜单名称
     */
    @Length(min = 0, max = 50, message = "菜单名称长度不能超过{max}个字符")
    @NotBlank(message = "菜单名称不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String menuName;
    /**
     * 显示顺序
     */
    @NotNull(message = "显示顺序不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Integer orderNum;
    /**
     * 路由地址
     */
    @Length(min = 0, max = 200, message = "路由地址不能超过{max}个字符")
    private String path;
    /**
     * 组件路径
     */
    @Length(min = 0, max = 200, message = "组件路径不能超过{max}个字符")
    private String component;
    /**
     * 路由参数
     */
    @JsonProperty("query")
    private String queryParam;
    /**
     * 是否为外链（0是 1否）
     */
    private String isFrame;
    /**
     * 是否缓存（0缓存 1不缓存）
     */
    private String isCache;
    /**
     * 菜单类型（M目录 C菜单 F按钮）
     */
    @NotBlank(message = "菜单类型（M目录 C菜单 F按钮）不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String menuType;
    /**
     * 显示状态（0显示 1隐藏）
     */
    private String visible;
    /**
     * 菜单状态（0正常 1停用）
     */
    private String status;
    /**
     * 权限标识
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Length(min = 0, max = 100, message = "权限标识长度不能超过{max}个字符")
    @Pattern(value = RegexConstants.PERMISSION_STRING, message = "权限标识必须符合 tool:build:list 格式")
    private String perms;
    /**
     * 菜单图标
     */
    private String icon;
    @Length(min = 0, max = 255, message = "激活菜单路径长度不能超过{max}个字符")
    private String activeMenu;
    @Length(min = 0, max = 2000, message = "扩展字段长度不能超过{max}个字符")
    private String ext;
    /**
     * 备注
     */
    private String remark;

}
