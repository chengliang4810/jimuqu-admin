package com.jimuqu.system.mapper;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.domain.vo.SysRoleVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 角色信息数据层。
 *
 * @author chengliang4810
 */
@Mapper
public interface SysRoleMapper extends BaseMapperPlus<SysRole, SysRoleVo> {

    /**
     * 根据用户ID查询已分配且未删除的角色。
     */
    default List<SysRoleVo> selectRolesByUserId(Long userId) {
        return QueryChain.of(this)
                .selectDistinct()
                .select(SysRole.class)
                .leftJoin(SysRole::getId, SysUserRole::getRoleId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getDelFlag, "0")
                .orderBy(SysRole::getRoleSort, SysRole::getId)
                .returnType(SysRoleVo.class)
                .list();
    }

    /**
     * 根据角色ID查询角色。
     */
    default SysRoleVo selectRoleById(Long roleId) {
        return QueryChain.of(this)
                .eq(SysRole::getId, roleId)
                .eq(SysRole::getDelFlag, "0")
                .returnType(SysRoleVo.class)
                .get();
    }
}
