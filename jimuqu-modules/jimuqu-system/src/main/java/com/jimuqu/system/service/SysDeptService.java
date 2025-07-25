package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.bo.SysDeptBo;
import com.jimuqu.system.domain.vo.SysDeptVo;
import com.jimuqu.system.domain.query.SysDeptQuery;
import cn.hutool.v7.core.tree.MapTree;

import java.util.Collection;
import java.util.List;

/**
 * 部门Service接口
 *
 * @author chengliang4810
 * @since 2025-06-04
 */
public interface SysDeptService {

    /**
     * 根据主键查询部门
     *
     * @param id 部门主键
     * @return {@link SysDeptVo } 部门视图对象
     */
   SysDeptVo queryById(Long id);

    /**
     * 查询部门分页列表
     *
     * @param query 查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysDeptVo }> 部门分页对象
     */
    Page<SysDeptVo> queryPageList(SysDeptQuery query, PageQuery pageQuery);

   /**
     * 查询部门列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysDeptVo }> 部门列表
     */
    List<SysDeptVo> queryList(SysDeptQuery query);

    /**
     * 查询部门树结构信息
     *
     * @param deptQuery 部门信息
     * @return 部门树信息集合
     */
    List<MapTree<Long>> selectDeptTreeList(SysDeptQuery deptQuery);

    /**
     * 构建前端所需要下拉树结构
     *
     * @param deptVoList 部门列表
     * @return 下拉树结构列表
     */
    List<MapTree<Long>> buildDeptTreeSelect(List<SysDeptVo> deptVoList);

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    List<Long> selectDeptListByRoleId(Long roleId);

    /**
     * 根据ID查询所有子部门数（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    long selectNormalChildrenDeptById(Long deptId);

    /**
     * 是否存在部门子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    boolean hasChildByDeptId(Long deptId);

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    boolean checkDeptExistUser(Long deptId);

    /**
     * 校验部门名称是否唯一
     *
     * @param deptQuery 部门信息
     * @return 结果
     */
    boolean checkDeptNameUnique(SysDeptQuery deptQuery);

    /**
     * 校验部门是否有数据权限
     *
     * @param deptId 部门id
     */
    void checkDeptDataScope(Long deptId);

    /**
     * 新增部门
     *
     * @param bo 部门业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysDeptBo bo);

    /**
     * 更新部门
     *
     * @param bo 部门业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysDeptBo bo);

    /**
     * 批量删除代码生成模板信息
     *
     * @param ids 部门主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);
}
