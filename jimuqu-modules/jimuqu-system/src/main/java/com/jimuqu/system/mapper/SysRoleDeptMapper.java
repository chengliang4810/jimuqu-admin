package com.jimuqu.system.mapper;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysRoleDept;

import java.util.List;

/**
 * 角色与部门关联表 数据层
 *
 * @author Lion Li,chengliang4810
 */
public interface SysRoleDeptMapper extends BaseMapperPlus<SysRoleDept, SysRoleDept> {

    /**
     * 根据角色ID查询部门ID列表
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    default List<Long> selectDeptIdsByRoleId(Long roleId) {
        return QueryChain.of(this)
                .select(SysRoleDept::getDeptId)
                .eq(SysRoleDept::getRoleId, roleId)
                .returnType(Long.class)
                .list();
    }

}
