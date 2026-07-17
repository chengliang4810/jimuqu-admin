package com.jimuqu.system.mapper;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.vo.SysUserVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


/**
 * 用户信息数据层
 * @author chengliang4810
 * @since 2025-06-05
 */
@Mapper
public interface SysUserMapper extends BaseMapperPlus<SysUser, SysUserVo> {


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
