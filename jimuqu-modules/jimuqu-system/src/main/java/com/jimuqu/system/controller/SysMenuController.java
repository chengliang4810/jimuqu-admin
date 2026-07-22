package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.lang.tree.Tree;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.JsonUtil;
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
import com.jimuqu.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotEmpty;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * 菜单权限 Controller。
 *
 * @author chengliang4810
 */
@Controller
@RequiredArgsConstructor
@Mapping("/system/menu")
public class SysMenuController extends BaseController {

    private final SysMenuService menuService;
    private final SysRoleService roleService;

    @Get
    @Mapping("/getRouters")
    public R<List<RouterVo>> getRouters() {
        List<SysMenu> menus = menuService.queryMenuTreeByUserId(LoginHelper.getUserId());
        return R.ok(menuService.buildMenus(menus));
    }

    @Get
    @Mapping("/list")
    @SaCheckPermission("system:menu:list")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    public List<SysMenuVo> list(SysMenuQuery query) {
        return menuService.queryList(query, LoginHelper.getUserId());
    }

    @Get
    @Mapping("/{menuId}")
    @SaCheckPermission("system:menu:query")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    public SysMenuVo getInfo(@NotNull(message = "菜单ID不能为空") Long menuId) {
        return menuService.queryById(menuId);
    }

    @Get
    @Mapping("/treeselect")
    @SaCheckPermission("system:menu:query")
    public R<List<Tree<Long>>> treeselect(SysMenuQuery query) {
        List<SysMenuVo> menus = menuService.queryListForTreeSelect(query, LoginHelper.getUserId());
        return R.ok(menuService.buildMenuTreeSelect(menus));
    }

    @Get
    @Mapping("/roleMenuTreeselect/{roleId}")
    @SaCheckPermission("system:menu:query")
    public R<MenuTreeSelectVo> roleMenuTreeselect(Long roleId) {
        roleService.checkRoleDataScope(roleId);
        List<SysMenuVo> menus = menuService.queryList(LoginHelper.getUserId());
        MenuTreeSelectVo selectVo = new MenuTreeSelectVo();
        selectVo.setCheckedKeys(menuService.queryMenuListByRoleId(roleId));
        selectVo.setMenus(menuService.buildMenuTreeSelect(menus));
        return R.ok(selectVo);
    }

    @NoRepeatSubmit
    @Post
    @Mapping
    @SaCheckPermission("system:menu:add")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    @Log(title = "菜单管理", businessType = BusinessType.ADD)
    public R<Void> add(@Body @Validated(AddGroup.class) SysMenuBo menu) {
        validateQueryParam(menu.getQueryParam());
        if (!menuService.checkMenuNameUnique(menu)) {
            return R.fail("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        if (isFrame(menu.getIsFrame()) && !StringUtil.isHttp(menu.getPath())) {
            return R.fail("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        if (!menuService.checkRouteConfigUnique(menu)) {
            return R.fail("新增菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        return toAjax(menuService.insertByBo(menu));
    }

    @Put
    @Mapping
    @NoRepeatSubmit
    @SaCheckPermission("system:menu:edit")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    public R<Void> edit(@Body @Validated(UpdateGroup.class) SysMenuBo menu) {
        validateQueryParam(menu.getQueryParam());
        if (!menuService.checkMenuNameUnique(menu)) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        if (isFrame(menu.getIsFrame()) && !StringUtil.isHttp(menu.getPath())) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        if (menu.getId().equals(menu.getParentId())) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        }
        if (!menuService.checkRouteConfigUnique(menu)) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        return toAjax(menuService.updateByBo(menu));
    }

    @Delete
    @Mapping("/{menuIds}")
    @SaCheckPermission("system:menu:remove")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    public R<Void> delete(@NotEmpty(message = "菜单ID不能为空") List<Long> menuIds) {
        if (menuService.hasChildByMenuId(menuIds)) {
            return R.warn("存在子菜单,不允许删除");
        }
        if (menuService.checkMenuExistRole(menuIds)) {
            return R.warn("菜单已分配,不允许删除");
        }
        Integer num = menuService.deleteById(menuIds);
        Assert.gtZero(num, "删除菜单失败");
        return R.ok();
    }

    @Delete
    @Mapping("/cascade/{menuIds}")
    @SaCheckPermission("system:menu:remove")
    @SaCheckRole(GlobalConstants.SUPER_ADMIN_ROLE_KEY)
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    public R<Void> cascadeDelete(@NotEmpty(message = "菜单ID不能为空") List<Long> menuIds) {
        if (menuService.hasChildByMenuId(menuIds)) {
            return R.warn("存在子菜单,不允许删除");
        }
        return toAjax(menuService.deleteById(menuIds));
    }

    private static boolean isFrame(String value) {
        return UserConstants.YES.equals(value) || UserConstants.YES_FRAME.equals(value);
    }

    static void validateQueryParam(String queryParam) {
        if (StringUtil.isBlank(queryParam)) {
            return;
        }
        try {
            if (JsonUtil.toObject(queryParam, Object.class) instanceof Map<?, ?>) {
                return;
            }
        } catch (RuntimeException ignored) {
            // 统一转换为与上游校验注解一致的业务提示。
        }
        throw new ServiceException("路由参数必须符合JSON格式");
    }
}
