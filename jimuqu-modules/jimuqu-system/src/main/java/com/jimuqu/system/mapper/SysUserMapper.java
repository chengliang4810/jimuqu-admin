package com.jimuqu.system.mapper;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.model.DataScopeWriteRule;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.vo.SysUserVo;
import org.apache.ibatis.annotations.Mapper;
import db.sql.api.impl.cmd.struct.Where;

import java.util.List;
import java.util.Objects;


/**
 * 用户信息数据层
 * @author chengliang4810
 * @since 2025-06-05
 */
@Mapper
public interface SysUserMapper extends BaseMapperPlus<SysUser, SysUserVo> {

    /**
     * 使用 typed WHERE 原子应用逐角色写数据权限。
     */
    default int updateWithDataScope(SysUser user, DataScopeWriteRule writeRule) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return update(user, where -> {
            where.eq(SysUser::getId, user.getId());
            applyWriteDataScope(where, writeRule);
        });
    }

    static void applyWriteDataScope(Where where, DataScopeWriteRule writeRule) {
        Objects.requireNonNull(where, "更新条件不能为空");
        Objects.requireNonNull(writeRule, "写数据权限规则不能为空");
        if (writeRule.allAccess()) {
            return;
        }
        if (writeRule.denyAll()) {
            appendDenyCondition(where);
            return;
        }
        for (DataScopeRule roleRule : writeRule.roleRules()) {
            boolean hasDepartments = !roleRule.departmentIds().isEmpty();
            boolean hasSelf = roleRule.selfAccess() && roleRule.userId() != null;
            if (hasDepartments && hasSelf) {
                where.andNested(scope -> scope
                        .in(SysUser::getDeptId, roleRule.departmentIds())
                        .or()
                        .eq(SysUser::getCreateBy, roleRule.userId()));
            } else if (hasDepartments) {
                where.andNested(scope -> scope.in(SysUser::getDeptId, roleRule.departmentIds()));
            } else if (hasSelf) {
                where.andNested(scope -> scope.eq(SysUser::getCreateBy, roleRule.userId()));
            } else {
                appendDenyCondition(where);
                return;
            }
        }
    }

    private static void appendDenyCondition(Where where) {
        where.andNested(scope -> scope
                .eq(SysUser::getId, 0L)
                .and()
                .ne(SysUser::getId, 0L));
    }


    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    default SysUserVo selectUserByUserName(String userName) {
        return QueryChain.of(this)
                .leftJoin(SysUser::getDeptId, SysDept::getId)
                .leftJoin(SysUser::getId, SysUserRole::getUserId)
                .leftJoin(SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUser::getUserName, userName)
                .returnType(SysUserVo.class).get();
    }

    /**
     * 通过手机号查询用户
     *
     * @param phonenumber 手机号
     * @return 用户对象信息
     */
    default SysUserVo selectUserByPhonenumber(String phonenumber) {
        return QueryChain.of(this)
                .leftJoin(SysUser::getDeptId, SysDept::getId)
                .leftJoin(SysUser::getId, SysUserRole::getUserId)
                .leftJoin(SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUser::getPhonenumber, phonenumber)
                .returnType(SysUserVo.class).get();
    }

    /**
     * 通过邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户对象信息
     */
    default SysUserVo selectUserByEmail(String email) {
        return QueryChain.of(this)
                .leftJoin(SysUser::getDeptId, SysDept::getId)
                .leftJoin(SysUser::getId, SysUserRole::getUserId)
                .leftJoin(SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUser::getEmail, email)
                .returnType(SysUserVo.class).get();
    }

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */

    default SysUserVo selectUserById(Long userId) {
        return QueryChain.of(this)
                .leftJoin(SysUser::getDeptId, SysDept::getId)
                .leftJoin(SysUser::getId, SysUserRole::getUserId)
                .leftJoin(SysUserRole::getRoleId, SysRole::getId)
                .eq(SysUser::getId, userId)
                .returnType(SysUserVo.class).get();
    }
}
