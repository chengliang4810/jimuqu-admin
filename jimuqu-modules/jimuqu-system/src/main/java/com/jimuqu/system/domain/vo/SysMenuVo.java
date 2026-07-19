package com.jimuqu.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import cn.xbatis.db.annotations.Ignores;
import cn.xbatis.db.annotations.ResultEntity;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.system.domain.SysMenu;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 菜单权限视图对象
 * @author chengliang4810
 * @since 2025-06-06
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@ResultEntity(SysMenu.class)
@AutoMapper(target = SysMenu.class)
@Ignores("children")
public class SysMenuVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     */
    @JsonProperty("menuId")
    @ONodeAttr(name = "menuId")
    private Long id;
    /**
     * 父菜单ID
     */
    private Long parentId;
    /**
     * 菜单名称
     */
    private String menuName;
    /**
     * 显示顺序
     */
    private Integer orderNum;
    /**
     * 路由地址
     */
    private String path;
    /**
     * 组件路径
     */
    private String component;
    /**
     * 路由参数
     */
    private String queryParam;
    /**
     * 是否为外链（Y是 N否）
     */
    private String isFrame;
    /**
     * 是否缓存（Y缓存 N不缓存）
     */
    private String isCache;
    /**
     * 菜单类型（M目录 C菜单 F按钮）
     */
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
    private String perms;
    /**
     * 菜单图标
     */
    private String icon;
    private String activeMenu;
    private String ext;
    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建部门
     */
    private Long createDept;

    /**
     * 子菜单
     */
    private List<SysMenuVo> children = new ArrayList<>();

    public String getIsFrame() {
        return UserConstants.YES_FRAME.equals(isFrame) ? UserConstants.YES
                : UserConstants.NO_FRAME.equals(isFrame) ? UserConstants.NO : isFrame;
    }

    public SysMenuVo setIsFrame(String isFrame) {
        this.isFrame = UserConstants.YES_FRAME.equals(isFrame) ? UserConstants.YES
                : UserConstants.NO_FRAME.equals(isFrame) ? UserConstants.NO : isFrame;
        return this;
    }

    public String getIsCache() {
        return "0".equals(isCache) ? UserConstants.YES
                : "1".equals(isCache) ? UserConstants.NO : isCache;
    }

    public SysMenuVo setIsCache(String isCache) {
        this.isCache = "0".equals(isCache) ? UserConstants.YES
                : "1".equals(isCache) ? UserConstants.NO : isCache;
        return this;
    }

}
