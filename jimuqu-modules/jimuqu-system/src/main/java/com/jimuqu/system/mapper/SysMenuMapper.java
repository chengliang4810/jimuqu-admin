package com.jimuqu.system.mapper;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysMenu;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysRoleMenu;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.query.SysMenuQuery;
import com.jimuqu.system.domain.vo.SysMenuVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 菜单权限数据层。
 *
 * @author chengliang4810
 */
@Mapper
public interface SysMenuMapper extends BaseMapperPlus<SysMenu, SysMenuVo> {

    default List<SysMenu> selectMenuAll() {
        return QueryChain.of(this)
                .in(SysMenu::getMenuType, UserConstants.TYPE_DIR, UserConstants.TYPE_MENU)
                .eq(SysMenu::getStatus, UserConstants.MENU_NORMAL)
                .orderBy(SysMenu::getParentId, SysMenu::getOrderNum)
                .list();
    }

    default List<SysMenu> selectMenuByUserId(Long userId) {
        return QueryChain.of(this)
                .selectDistinct()
                .select(SysMenu.class)
                .leftJoin(SysMenu::getId, SysRoleMenu::getMenuId)
                .leftJoin(SysRoleMenu::getRoleId, SysUserRole::getRoleId)
                .leftJoin(SysRoleMenu::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId)
                .in(SysMenu::getMenuType, UserConstants.TYPE_DIR, UserConstants.TYPE_MENU)
                .eq(SysMenu::getStatus, UserConstants.MENU_NORMAL)
                .eq(SysRole::getStatus, UserConstants.ROLE_NORMAL)
                .eq(SysRole::getDelFlag, "0")
                .orderBy(SysMenu::getParentId, SysMenu::getOrderNum)
                .list();
    }

    default List<SysMenuVo> selectMenuListByUserId(Long userId, SysMenuQuery query) {
        return QueryChain.of(this)
                .selectDistinct()
                .select(SysMenu.class)
                .leftJoin(SysMenu::getId, SysRoleMenu::getMenuId)
                .leftJoin(SysRoleMenu::getRoleId, SysUserRole::getRoleId)
                .leftJoin(SysRoleMenu::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getStatus, UserConstants.ROLE_NORMAL)
                .eq(SysRole::getDelFlag, "0")
                .where(query)
                .orderBy(SysMenu::getParentId, SysMenu::getOrderNum)
                .returnType(SysMenuVo.class)
                .list();
    }

    default List<String> selectMenuPermsByUserId(Long userId) {
        return QueryChain.of(this)
                .selectDistinct()
                .select(SysMenu::getPerms)
                .leftJoin(SysMenu::getId, SysRoleMenu::getMenuId)
                .leftJoin(SysRoleMenu::getRoleId, SysUserRole::getRoleId)
                .leftJoin(SysRoleMenu::getRoleId, SysRole::getId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysMenu::getStatus, UserConstants.MENU_NORMAL)
                .eq(SysRole::getStatus, UserConstants.ROLE_NORMAL)
                .eq(SysRole::getDelFlag, "0")
                .returnType(String.class)
                .list();
    }

    default List<String> selectMenuPermsByRoleId(Long roleId) {
        return QueryChain.of(this)
                .selectDistinct()
                .select(SysMenu::getPerms)
                .leftJoin(SysMenu::getId, SysRoleMenu::getMenuId)
                .eq(SysRoleMenu::getRoleId, roleId)
                .eq(SysMenu::getStatus, UserConstants.MENU_NORMAL)
                .returnType(String.class)
                .list();
    }

    default List<Long> selectMenuListByRoleId(Long roleId, boolean menuCheckStrictly) {
        List<SysMenu> selected = QueryChain.of(this)
                .select(SysMenu::getId, SysMenu::getParentId)
                .leftJoin(SysMenu::getId, SysRoleMenu::getMenuId)
                .eq(SysRoleMenu::getRoleId, roleId)
                .orderBy(SysMenu::getParentId, SysMenu::getOrderNum)
                .list();
        Set<Long> selectedParentIds = new HashSet<>();
        if (menuCheckStrictly) {
            selected.stream().map(SysMenu::getParentId).forEach(selectedParentIds::add);
        }
        return selected.stream()
                .map(SysMenu::getId)
                .filter(id -> !selectedParentIds.contains(id))
                .toList();
    }
}
