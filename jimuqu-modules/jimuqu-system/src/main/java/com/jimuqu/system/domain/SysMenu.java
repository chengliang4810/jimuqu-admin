package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Ignores;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import me.zhyd.oauth.utils.StringUtils;
import org.dromara.autotable.annotation.AutoColumn;
import cn.hutool.v7.core.lang.Validator;
import cn.hutool.v7.core.text.StrPool;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单权限
 * @author chengliang4810
 * @since 2025-06-06
 */
@Data
@Table("sys_menu")
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Ignores({SysMenu.Fields.parentName, SysMenu.Fields.children})
public class SysMenu extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "菜单ID")
    private Long id;
    /**
     * 父菜单ID
     */
    @AutoColumn(comment = "父菜单ID", defaultValue = "0")
    private Long parentId;
    /**
     * 菜单名称
     */
    @AutoColumn(comment = "菜单名称", length = 50)
    private String menuName;
    /**
     * 显示顺序
     */
    @AutoColumn(comment = "显示顺序", defaultValue = "0")
    private Integer orderNum;
    /**
     * 路由地址
     */
    @AutoColumn(comment = "路由地址", length = 200)
    private String path;
    /**
     * 组件路径
     */
    @AutoColumn(comment = "组件路径", length = 255)
    private String component;
    /**
     * 路由参数
     */
    @AutoColumn(comment = "路由参数", length = 255)
    private String queryParam;
    /**
     * 是否为外链（0是 1否）
     */
    @AutoColumn(comment = "是否为外链（0是 1否）", defaultValue = "1")
    private String isFrame;
    /**
     * 是否缓存（0缓存 1不缓存）
     */
    @AutoColumn(comment = "是否缓存（0缓存 1不缓存）", defaultValue = "0")
    private String isCache;
    /**
     * 菜单类型（M目录 C菜单 F按钮）
     */
    @AutoColumn(comment = "菜单类型（M目录 C菜单 F按钮）", length = 1)
    private String menuType;
    /**
     * 显示状态（0显示 1隐藏）
     */
    @AutoColumn(comment = "显示状态（0显示 1隐藏）", length = 1, defaultValue = "0")
    private String visible;
    /**
     * 菜单状态（0正常 1停用）
     */
    @AutoColumn(comment = "菜单状态（0正常 1停用）", length = 1, defaultValue = "0")
    private String status;
    /**
     * 权限标识
     */
    @AutoColumn(comment = "权限标识", length = 100)
    private String perms;
    /**
     * 菜单图标
     */
    @AutoColumn(comment = "菜单图标", length = 100, defaultValue = "#")
    private String icon;
    /** 激活菜单路径。 */
    @AutoColumn(comment = "激活菜单路径", length = 255)
    private String activeMenu;
    /** Vben 路由扩展 JSON。 */
    @AutoColumn(comment = "扩展字段", length = 2000)
    private String ext;
    /**
     * 备注
     */
    @AutoColumn(comment = "备注", length = 500)
    private String remark;

    /**
     * 父菜单名称
     */
    private String parentName;

    /**
     * 子菜单
     */
    private List<SysMenu> children = new ArrayList<>();

    /**
     * 内链域名特殊字符替换
     */
    public static String innerLinkReplaceEach(String path) {
        path = StringUtil.removeAll(path, Constants.HTTP, Constants.HTTPS, Constants.WWW);
        path = StringUtil.replace(path, StrPool.COLON , StrPool.SLASH);
        path = StringUtil.replace(path, StrPool.DOT , StrPool.SLASH);
        return path;
    }

    /**
     * 获取路由名称
     */
    public String getRouteName() {
        String routerName = StringUtil.upperFirst(path);
        // 非外链并且是一级目录（类型为目录）
        if (isMenuFrame()) {
            routerName = StringUtil.EMPTY;
        }
        return routerName;
    }

    /**
     * 获取路由地址
     */
    public String getRouterPath() {
        String routerPath = this.path;
        // 内链打开外网方式
        if (getParentId() != 0L && isInnerLink()) {
            routerPath = innerLinkReplaceEach(routerPath);
        }
        // 非外链并且是一级目录（类型为目录）
        if (0L == getParentId() && UserConstants.TYPE_DIR.equals(getMenuType())
                && UserConstants.NO_FRAME.equals(getIsFrame())) {
            routerPath = "/" + this.path;
        }
        // 非外链并且是一级目录（类型为菜单）
        else if (isMenuFrame()) {
            routerPath = "/";
        }
        return routerPath;
    }

    /**
     * 获取组件信息
     */
    public String getComponentInfo() {
        String component = UserConstants.LAYOUT;
        if (StringUtils.isNotEmpty(this.component) && !isMenuFrame()) {
            component = this.component;
        } else if (StringUtils.isEmpty(this.component) && getParentId() != 0L && isInnerLink()) {
            component = UserConstants.INNER_LINK;
        } else if (StringUtils.isEmpty(this.component) && isParentView()) {
            component = UserConstants.PARENT_VIEW;
        }
        return component;
    }

    /**
     * 是否为菜单内部跳转
     */
    public boolean isMenuFrame() {
        return getParentId() == 0L && UserConstants.TYPE_MENU.equals(menuType) && isFrame.equals(UserConstants.NO_FRAME);
    }

    /**
     * 是否为内链组件
     */
    public boolean isInnerLink() {
        return isFrame.equals(UserConstants.NO_FRAME) && Validator.isUrl(path);
    }

    /**
     * 是否为parent_view组件
     */
    public boolean isParentView() {
        return getParentId() != 0L && UserConstants.TYPE_DIR.equals(menuType);
    }

}
