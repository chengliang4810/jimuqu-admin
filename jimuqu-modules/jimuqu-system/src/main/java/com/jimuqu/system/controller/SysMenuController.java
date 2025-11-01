package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.v7.core.tree.MapTree;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.SysMenu;
import com.jimuqu.system.domain.bo.SysMenuBo;
import com.jimuqu.system.domain.query.SysMenuQuery;
import com.jimuqu.system.domain.vo.MenuTreeSelectVo;
import com.jimuqu.system.domain.vo.RouterVo;
import com.jimuqu.system.domain.vo.SysMenuVo;
import com.jimuqu.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 菜单权限 Controller
 *
 * @author chengliang4810
 * @since 2025-06-06
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/menu")
public class SysMenuController extends BaseController {

    private final SysMenuService sysMenuService;

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @Get
    @Mapping("/routers" )
    public R<List<RouterVo>> getRouters() {
        List<SysMenu> menus = sysMenuService.queryMenuTreeByUserId(LoginHelper.getUserId());
        return R.ok(sysMenuService.buildMenus(menus));
    }

    /**
     * 查询菜单权限列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:menu:list")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    public List<SysMenuVo> list(SysMenuQuery query) {
        return sysMenuService.queryList(query, LoginHelper.getUserId());
    }

    /**
     * 获取菜单权限详细信息
     *
     * @param id 菜单权限主键
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:menu:query")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    public SysMenuVo getInfo(@NotNull(message = "菜单权限主键不能为空") Long id) {
        return sysMenuService.queryById(id);
    }

    /**
     * 获取菜单下拉树列表
     */
    @Get
    @SaCheckPermission("system:menu:query" )
    @Mapping("/treeselect" )
    public R<List<MapTree<Long>>> treeselect(SysMenuQuery menuQuery) {
        List<SysMenuVo> menus = sysMenuService.queryListForTreeSelect(menuQuery, LoginHelper.getUserId());
        return R.ok(sysMenuService.buildMenuTreeSelect(menus));
    }

    /**
     * 加载对应角色菜单列表树
     *
     * @param roleId 角色ID
     */

    @Get
    @SaCheckPermission("system:menu:query" )
    @Mapping("/roleMenuTreeSelect/{roleId}" )
    public R<MenuTreeSelectVo> roleMenuTreeselect(Long roleId) {
        List<SysMenuVo> menus = sysMenuService.queryList(LoginHelper.getUserId());
        MenuTreeSelectVo selectVo = new MenuTreeSelectVo();
        selectVo.setCheckedKeys(sysMenuService.queryMenuListByRoleId(roleId));
        selectVo.setMenus(sysMenuService.buildMenuTreeSelect(menus));
        return R.ok(selectVo);
    }

    /**
     * 新增菜单权限
     */
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("system:menu:add")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    @Log(title = "新增菜单权限", businessType = BusinessType.ADD)
    public R<Long> add(@Validated(AddGroup.class) SysMenuBo menu) {
        if (!sysMenuService.checkMenuNameUnique(menu)) {
            return R.fail("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在" );
        } else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtil.isHttp(menu.getPath())) {
            return R.fail("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头" );
        }
        boolean result = sysMenuService.insertByBo(menu);
        Assert.isTrue(result, "新增菜单权限失败");
        return R.ok(menu.getId());
    }

    /**
     * 更新菜单权限
     */
    @NoRepeatSubmit
    @Mapping("/update")
    @SaCheckPermission("system:menu:update")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    @Log(title = "更新菜单权限", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Validated(UpdateGroup.class) SysMenuBo menu) {
        if (!sysMenuService.checkMenuNameUnique(menu)) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在" );
        } else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtil.isHttp(menu.getPath())) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头" );
        } else if (menu.getId().equals(menu.getParentId())) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己" );
        }
        boolean result = sysMenuService.updateByBo(menu);
        Assert.isTrue(result, "更新菜单权限失败");
        return R.ok();
    }

    /**
     * 删除菜单权限
     */
    @Mapping("/delete/{ids}")
    @SaCheckPermission("system:menu:delete")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    @Log(title = "删除菜单权限", businessType = BusinessType.DELETE)
    public R<Integer> delete(@NotEmpty(message = "主键不能为空") List<Long> ids) {
        if (sysMenuService.hasChildByMenuId(ids)) {
            return R.warn("存在子菜单,不允许删除");
        }
        Integer num = sysMenuService.deleteById(ids);
        Assert.gtZero(num, "删除菜单权限失败");
        return R.ok(num);
    }

}