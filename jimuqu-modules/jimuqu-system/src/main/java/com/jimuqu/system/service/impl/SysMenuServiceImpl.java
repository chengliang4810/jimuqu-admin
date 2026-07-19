package com.jimuqu.system.service.impl;

import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.tree.MapTree;
import cn.hutool.v7.core.util.ObjUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.utils.StreamUtil;
import com.jimuqu.common.core.utils.TreeBuildUtil;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysMenu;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysRoleMenu;
import com.jimuqu.system.domain.bo.SysMenuBo;
import com.jimuqu.system.domain.query.SysMenuQuery;
import com.jimuqu.system.domain.vo.MetaVo;
import com.jimuqu.system.domain.vo.RouterVo;
import com.jimuqu.system.domain.vo.SysMenuVo;
import com.jimuqu.system.mapper.SysMenuMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysRoleMenuMapper;
import com.jimuqu.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * 菜单权限Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-06-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /**
     * 查询菜单权限
     */
    @Override
    public SysMenuVo queryById(Long id) {
        return normalizeYesNo(sysMenuMapper.getVoById(id));
    }

    /**
     * 查询菜单权限列表
     */
    @Override
    public List<SysMenuVo> queryList(SysMenuQuery query, Long userId) {
        if (LoginHelper.isSuperAdmin(userId)) {
            return normalizeYesNo(buildQueryChain(query).returnType(SysMenuVo.class).list());
        }
        return normalizeYesNo(sysMenuMapper.selectMenuListByUserId(userId, query));
    }

    /**
     * 查询菜单权限列表（包含所有类型，用于 treeselect）
     */
    @Override
    public List<SysMenuVo> queryListForTreeSelect(SysMenuQuery query, Long userId) {
        return queryList(query, userId);
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysMenu> buildQueryChain(SysMenuQuery query) {
        return QueryChain.of(sysMenuMapper)
                .forSearch(true)
                .where(query)
                .orderBy(SysMenu::getParentId, SysMenu::getOrderNum)
                ;
    }

    /**
     * 新增菜单权限
     */
    @Override
    public Boolean insertByBo(SysMenuBo bo) {
        SysMenu sysMenu = MapstructUtil.convert(bo, SysMenu.class);
        normalizeYesNo(sysMenu);
        boolean flag = sysMenuMapper.save(sysMenu) > 0;
        bo.setId(sysMenu.getId());
        return flag;
    }

    /**
     * 修改菜单权限
     */
    @Override
    public Boolean updateByBo(SysMenuBo bo) {
        SysMenu sysMenu = MapstructUtil.convert(bo, SysMenu.class);
        normalizeYesNo(sysMenu);
        return sysMenuMapper.update(sysMenu) > 0;
    }

    private void normalizeYesNo(SysMenu menu) {
        menu.setIsFrame(normalizeYesNo(menu.getIsFrame(), UserConstants.NO, "是否外链"));
        menu.setIsCache(normalizeYesNo(menu.getIsCache(), UserConstants.YES, "是否缓存"));
    }

    private SysMenuVo normalizeYesNo(SysMenuVo menu) {
        if (menu != null) {
            menu.setIsFrame(menu.getIsFrame());
            menu.setIsCache(menu.getIsCache());
        }
        return menu;
    }

    private List<SysMenuVo> normalizeYesNo(List<SysMenuVo> menus) {
        menus.forEach(this::normalizeYesNo);
        return menus;
    }

    private String normalizeYesNo(String value, String defaultValue, String fieldName) {
        if (StringUtil.isBlank(value)) {
            return defaultValue;
        }
        return switch (value) {
            case UserConstants.YES, "0" -> UserConstants.YES;
            case UserConstants.NO, "1" -> UserConstants.NO;
            default -> throw new ServiceException(fieldName + "必须为Y或N");
        };
    }

    /**
     * 删除菜单信息
     *
     * @param menuIdList 菜单ID
     * @return {@link Integer } 删除成功条数
     */
    @Override
    @Transaction
    public Integer deleteById(List<Long> menuIdList) {
        List<Long> requested = menuIdList.stream().distinct().toList();
        long existing = QueryChain.of(sysMenuMapper).in(SysMenu::getId, requested).count();
        if (existing != requested.size()) {
            throw new ServiceException("菜单不存在");
        }
        int num = sysMenuMapper.deleteByIds(requested);
        sysRoleMenuMapper.deleteByMenuIds(requested);
        return num;
    }

    /**
     * 根据用户查询系统菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenuVo> queryList(Long userId) {
        return queryList(new SysMenuQuery(), userId);
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> queryMenuPermsByUserId(Long userId) {
        return splitPermissions(sysMenuMapper.selectMenuPermsByUserId(userId));
    }

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public Set<String> queryMenuPermsByRoleId(Long roleId) {
        return splitPermissions(sysMenuMapper.selectMenuPermsByRoleId(roleId));
    }

    /**
     * 根据用户ID查询菜单树信息
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> queryMenuTreeByUserId(Long userId) {
        List<SysMenu> menus;
        if (LoginHelper.isSuperAdmin(userId)) {
            menus = sysMenuMapper.selectMenuAll();
        } else {
            menus = sysMenuMapper.selectMenuByUserId(userId);
        }
        return getChildPerms(menus, 0);
    }

    /**
     * 根据用户ID查询菜单信息
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> queryMenuByUserId(Long userId) {
        if (LoginHelper.isSuperAdmin(userId)) {
            return sysMenuMapper.selectMenuAll();
        }
        return sysMenuMapper.selectMenuByUserId(userId);
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Long> queryMenuListByRoleId(Long roleId) {
        SysRole role = roleMapper.getById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        return sysMenuMapper.selectMenuListByRoleId(roleId, Boolean.TRUE.equals(role.getMenuCheckStrictly()));
    }

    /**
     * 构建前端路由所需要的菜单
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    @Override
    public List<RouterVo> buildMenus(List<SysMenu> menus) {
        List<RouterVo> routers = new LinkedList<>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(menu.getRouteName() + menu.getId());
            router.setPath(menu.getRouterPath());
            router.setComponent(menu.getComponentInfo());
            router.setQuery(menu.getQueryParam());
            router.setExt(menu.getExt());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), isNoCache(menu.getIsCache()), menu.getPath(), menu.getActiveMenu()));
            List<SysMenu> cMenus = menu.getChildren();
            if (CollUtil.isNotEmpty(cMenus) && UserConstants.TYPE_DIR.equals(menu.getMenuType())) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(cMenus));
            } else if (menu.isMenuFrame()) {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(StrUtil.upperFirst(menu.getPath()) + menu.getId());
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), isNoCache(menu.getIsCache()), menu.getPath(), menu.getActiveMenu()));
                children.setQuery(menu.getQueryParam());
                children.setExt(menu.getExt());
                childrenList.add(children);
                router.setChildren(childrenList);
            } else if (menu.getParentId().intValue() == 0 && menu.isInnerLink()) {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                String routerPath = SysMenu.innerLinkReplaceEach(menu.getPath());
                children.setPath(routerPath);
                children.setComponent(UserConstants.INNER_LINK);
                children.setName(StrUtil.upperFirst(routerPath) + menu.getId());
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                children.setExt(menu.getExt());
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    static boolean isNoCache(String value) {
        return UserConstants.NO.equals(value) || "1".equals(value);
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<MapTree<Long>> buildMenuTreeSelect(List<SysMenuVo> menus) {
        if (CollUtil.isEmpty(menus)) {
            return ListUtil.zero();
        }
        // 获取当前列表中每一个节点的parentId，然后在列表中查找是否有id与其parentId对应，若无对应，则表明此时节点列表中，该节点在当前列表中属于顶级节点
        List<MapTree<Long>> treeList = new ArrayList<>();
        for (SysMenuVo menu : menus) {
            Long parentId = menu.getParentId();
            SysMenuVo parentMenu = StreamUtil.findFirst(menus, it -> it.getId().longValue() == parentId);
            if (ObjUtil.isNull(parentMenu)) {
                List<MapTree<Long>> trees = TreeBuildUtil.build(menus, parentId, (m, tree) -> {
                    tree.setId(m.getId())
                            .setParentId(m.getParentId())
                            .setName(m.getMenuName())
                            .setWeight(m.getOrderNum());
                    tree.putExtra("menuType", m.getMenuType());
                    tree.putExtra("perms", m.getPerms());
                    tree.putExtra("icon", m.getIcon());
                    tree.putExtra("visible", m.getVisible());
                    tree.putExtra("status", m.getStatus());
                });
                MapTree<Long> tree = StreamUtil.findFirst(trees, it -> it.getId().longValue() == menu.getId());
                treeList.add(tree);
            }
        }
        return treeList;
    }

    /**
     * 是否存在菜单子节点
     *
     * @param menuIdList 菜单ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean hasChildByMenuId(List<Long> menuIdList) {
        return sysMenuMapper.exists(where -> where.in(SysMenu::getParentId, menuIdList).notIn(SysMenu::getId, menuIdList));
    }

    /**
     * 查询菜单是否存在角色
     *
     * @param menuId 菜单ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkMenuExistRole(Long menuId) {
        return sysRoleMenuMapper.exists(where -> where.eq(SysRoleMenu::getMenuId, menuId));
    }

    @Override
    public boolean checkMenuExistRole(List<Long> menuIds) {
        return CollUtil.isNotEmpty(menuIds)
                && sysRoleMenuMapper.exists(where -> where.in(SysRoleMenu::getMenuId, menuIds));
    }

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkMenuNameUnique(SysMenuBo menu) {
        return !sysMenuMapper.exists(where -> where
                .eq(SysMenu::getMenuName, menu.getMenuName())
                .eq(SysMenu::getParentId, menu.getParentId())
                .ne(menu.getId() != null, SysMenu::getId, menu.getId())
        );
    }

    /**
     * 根据父节点的ID获取所有子节点
     *
     * @param list     分类表
     * @param parentId 传入的父节点ID
     * @return String
     */
    private List<SysMenu> getChildPerms(List<SysMenu> list, int parentId) {
        List<SysMenu> returnList = new ArrayList<>();
        for (SysMenu t : list) {
            // 一、根据传入的某个父节点ID,遍历该父节点的所有子节点
            if (Objects.equals(t.getParentId(), (long) parentId)) {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<SysMenu> list, SysMenu t) {
        // 得到子节点列表
        List<SysMenu> childList = StreamUtil.filter(list, n -> n.getParentId().equals(t.getId()));
        t.setChildren(childList);
        for (SysMenu tChild : childList) {
            // 判断是否有子节点
            if (list.stream().anyMatch(n -> n.getParentId().equals(tChild.getId()))) {
                recursionFn(list, tChild);
            }
        }
    }

    @Override
    public boolean checkRouteConfigUnique(SysMenuBo bo) {
        if (UserConstants.TYPE_BUTTON.equals(bo.getMenuType())) {
            return true;
        }
        SysMenu candidate = MapstructUtil.convert(bo, SysMenu.class);
        String path = StringUtil.defaultIfBlank(candidate.getPath(), "");
        String computedRouteName = candidate.getRouteName();
        String routeName = StringUtil.isBlank(computedRouteName) ? path : computedRouteName;
        return QueryChain.of(sysMenuMapper)
                .in(SysMenu::getMenuType, List.of(UserConstants.TYPE_DIR, UserConstants.TYPE_MENU))
                .list().stream()
                .filter(menu -> !Objects.equals(menu.getId(), bo.getId()))
                .noneMatch(menu -> {
                    String storedPath = StringUtil.defaultIfBlank(menu.getPath(), "");
                    String storedComputedRouteName = menu.getRouteName();
                    String storedRouteName = StringUtil.isBlank(storedComputedRouteName)
                            ? storedPath : storedComputedRouteName;
                    return (Objects.equals(menu.getParentId(), candidate.getParentId())
                            && path.equalsIgnoreCase(storedPath))
                            || (Objects.equals(menu.getMenuType(), candidate.getMenuType())
                            && routeName.equalsIgnoreCase(storedRouteName));
                });
    }

    private Set<String> splitPermissions(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return Collections.emptySet();
        }
        Set<String> permissions = new HashSet<>();
        for (String value : values) {
            if (StringUtil.isNotBlank(value)) {
                permissions.addAll(StringUtil.splitList(value.trim()));
            }
        }
        return permissions;
    }
}
