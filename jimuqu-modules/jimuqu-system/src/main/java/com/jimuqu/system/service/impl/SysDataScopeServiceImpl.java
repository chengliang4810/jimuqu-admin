package com.jimuqu.system.service.impl;

import cn.hutool.v7.core.collection.CollUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysRoleDept;
import com.jimuqu.system.domain.vo.SysRoleVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysRoleDeptMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统数据权限服务实现类
 * <p>
 * Bean名称必须为 "sysDataScopeService" 以便SpEL表达式调用
 *
 * @author chengliang4810
 * @version 1.0
 */
@Slf4j
@Component("sysDataScopeService")
@RequiredArgsConstructor
public class SysDataScopeServiceImpl implements ISysDataScopeService {

    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysRoleDeptMapper sysRoleDeptMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 获取角色自定义权限部门ID列表
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    @Override
    public List<Long> getRoleCustom(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }

        // 查询角色的自定义部门权限
        return QueryChain.of(sysRoleDeptMapper)
                .select(SysRoleDept::getDeptId)
                .eq(SysRoleDept::getRoleId, roleId)
                .returnType(Long.class)
                .list();
    }

    /**
     * 获取部门及以下部门ID列表
     *
     * @param deptId 部门ID
     * @return 部门ID列表
     */
    @Override
    public List<Long> getDeptAndChild(Long deptId) {
        if (deptId == null) {
            return new ArrayList<>();
        }

        List<Long> deptIds = new ArrayList<>();
        deptIds.add(deptId);

        // 递归查询所有子部门
        List<Long> childDeptIds = findChildDeptIds(deptId);
        if (CollUtil.isNotEmpty(childDeptIds)) {
            deptIds.addAll(childDeptIds);
        }

        return deptIds;
    }

    /**
     * 获取用户数据权限部门ID列表
     *
     * @param userId 用户ID
     * @return 部门ID列表
     */
    @Override
    public List<Long> getUserDataScope(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        // 获取用户的角色信息
        SysUserVo user = sysUserMapper.selectUserById(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        // 获取用户的角色数据权限
        String dataScope = getUserDataScopeFromRoles(user.getRoles());
        if (dataScope == null || dataScope.isEmpty()) {
            return new ArrayList<>();
        }

        // 根据数据权限范围返回部门列表
        return switch (dataScope) {
            case "1" -> // 全部数据权限
                QueryChain.of(sysDeptMapper).select(SysDept::getId).returnType(Long.class).list();
            case "2" -> // 自定数据权限
                getRoleCustom(user.getRoleId());
            case "3" -> // 部门数据权限
                List.of(user.getDeptId());
            case "4" -> // 部门及以下数据权限
                getDeptAndChild(user.getDeptId());
            case "5" -> // 仅本人数据权限
                new ArrayList<>(); // 返回空列表，表示只能看自己
            default -> new ArrayList<>();
        };
    }

    /**
     * 从用户角色中获取数据权限范围
     *
     * @param roles 用户角色列表
     * @return 数据权限范围
     */
    private String getUserDataScopeFromRoles(List<SysRoleVo> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }

        // 如果有任何一个角色拥有全部数据权限，则返回全部权限
        for (SysRoleVo role : roles) {
            if ("1".equals(role.getDataScope())) {
                return "1";
            }
        }

        // 按权限级别从高到低排序
        String[] priorityOrder = {"1", "2", "3", "4", "5"};
        for (String scope : priorityOrder) {
            for (SysRoleVo role : roles) {
                if (scope.equals(role.getDataScope())) {
                    return scope;
                }
            }
        }

        return "";
    }

    /**
     * 检查用户是否有部门数据权限
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 是否有权限
     */
    @Override
    public boolean checkUserDataScope(Long userId, Long deptId) {
        if (userId == null || deptId == null) {
            return false;
        }

        // 获取用户的数据权限部门列表
        List<Long> allowedDeptIds = getUserDataScope(userId);
        return allowedDeptIds.contains(deptId);
    }

    /**
     * 递归查找子部门ID列表
     *
     * @param parentId 父部门ID
     * @return 子部门ID列表
     */
    private List<Long> findChildDeptIds(Long parentId) {
        List<Long> childIds = new ArrayList<>();

        // 查询直接子部门
        List<Long> directChildren = QueryChain.of(sysDeptMapper)
                .select(SysDept::getId)
                .eq(SysDept::getParentId, parentId)
                .returnType(Long.class)
                .list();

        if (CollUtil.isNotEmpty(directChildren)) {
            childIds.addAll(directChildren);

            // 递归查询子部门的子部门
            for (Long childId : directChildren) {
                List<Long> grandChildren = findChildDeptIds(childId);
                childIds.addAll(grandChildren);
            }
        }

        return childIds;
    }

}