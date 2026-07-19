package com.jimuqu.system.mapper;

import cn.xbatis.core.sql.executor.Where;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysRoleDept;
import com.jimuqu.system.domain.vo.SysDeptVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 部门数据层
 * @author chengliang4810
 * @since 2025-06-04
 */
@Mapper
public interface SysDeptMapper extends BaseMapperPlus<SysDept, SysDeptVo> {

    /**
     * 根据父部门ID查询其所有子部门的ID列表
     *
     * @param parentId 父部门ID
     * @return 包含子部门的列表
     */
    default List<Long> selectListByParentId(Long parentId) {
        return QueryChain.of(this)
                .select(SysDept::getId)
                .and(SysDept::getAncestors, dept-> dept.mysql().findInSet(parentId))
                .returnType(Long.class)
                .list();
    }


    /**
     * 查询部门管理数据
     *
     * @param queryWrapper 查询条件
     * @return 部门信息集合
     */

    default List<SysDeptVo> selectDeptList(Where queryWrapper) {
        return QueryChain.of(this, queryWrapper)
                .where(queryWrapper)
                .orderBy(SysDept::getAncestors, SysDept::getParentId, SysDept::getOrderNum, SysDept::getId)
                .returnType(SysDeptVo.class)
                .list();
    }

    default SysDeptVo selectDeptById(Long deptId) {
        return QueryChain.of(this)
                .eq(SysDept::getId, deptId)
                .eq(SysDept::getDelFlag, "0")
                .returnType(SysDeptVo.class)
                .get();
    }


    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId            角色ID
     * @param deptCheckStrictly 部门树选择项是否关联显示
     * @return 选中部门列表
     */
    default List<Long> selectDeptListByRoleId(Long roleId, boolean deptCheckStrictly) {
        List<SysDept> selected = QueryChain.of(this)
                .select(SysDept::getId, SysDept::getParentId)
                .leftJoin(SysDept::getId, SysRoleDept::getDeptId)
                .leftJoin(SysRoleDept::getRoleId, SysRole::getId)
                .eq(SysRoleDept::getRoleId, roleId)
                .eq(SysRole::getStatus, "0")
                .eq(SysRole::getDelFlag, "0")
                .orderBy(SysDept::getParentId, SysDept::getOrderNum)
                .list();
        Set<Long> selectedParentIds = new HashSet<>();
        if (deptCheckStrictly) {
            selected.stream()
                    .map(SysDept::getParentId)
                    .forEach(selectedParentIds::add);
        }
        return selected.stream()
                .map(SysDept::getId)
                .filter(id -> !selectedParentIds.contains(id))
                .toList();
    }

    /**
     * 统计部门数
     * @param deptId 部门ID
     * @return 结果
     */
    default int countDeptById(Long deptId){
        return this.count(Where.create().eq(SysDept::getDelFlag, "0").eq(SysDept::getId, deptId));
    }
}
