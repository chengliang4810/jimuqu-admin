package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StreamUtil;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysMenu;
import com.jimuqu.system.domain.SysRoleMenu;
import com.jimuqu.system.domain.bo.SysMenuBo;
import com.jimuqu.system.domain.query.SysMenuQuery;
import com.jimuqu.system.domain.vo.MetaVo;
import com.jimuqu.system.domain.vo.RouterVo;
import com.jimuqu.system.domain.vo.SysMenuVo;
import com.jimuqu.system.mapper.SysMenuMapper;
import com.jimuqu.system.mapper.SysRoleMenuMapper;
import com.jimuqu.system.service.SysMenuService;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.tree.MapTree;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /**
     * 查询菜单权限
     */
    @Override
    public SysMenuVo queryById(Long id) {
        return sysMenuMapper.getVoById(id);
    }

    /**
     * 查询菜单权限列表
     */
    @Override
    public List<SysMenuVo> queryList(SysMenuQuery query, Long userId) {
        QueryChain<SysMenu> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysMenuVo.class).list();
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
        return sysMenuMapper.update(sysMenu) > 0;
    }

    /**
     * 删除代码生成模板信息
     *
     * @param menuIdList 菜单ID
     * @return {@link Integer } 删除成功条数
     */
    @Override
    @Transaction
    public Integer deleteById(List<Long> menuIdList) {
        int num = sysMenuMapper.deleteByIds(menuIdList);
        sysRoleMenuMapper.deleteByMenuIds(menuIdList);
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
        return List.of();
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> queryMenuPermsByUserId(Long userId) {
        return Set.of();
    }

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public Set<String> queryMenuPermsByRoleId(Long roleId) {
        return Set.of();
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
        return List.of();
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Long> queryMenuListByRoleId(Long roleId) {
        return List.of();
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
            router.setName(menu.getRouteName());
            router.setPath(menu.getRouterPath());
            router.setComponent(menu.getComponentInfo());
            router.setQuery(menu.getQueryParam());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtil.equals("1", menu.getIsCache()), menu.getPath()));
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
                children.setName(StringUtil.capitalize(menu.getPath()));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtil.equals("1", menu.getIsCache()), menu.getPath()));
                children.setQuery(menu.getQueryParam());
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
                children.setName(StringUtil.capitalize(routerPath));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<MapTree<Long>> buildMenuTreeSelect(List<SysMenuVo> menus) {
        return List.of();
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
            if (t.getParentId() == parentId) {
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
}
