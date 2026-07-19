package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.bo.SysPostBo;
import com.jimuqu.system.domain.vo.SysPostVo;
import com.jimuqu.system.domain.query.SysPostQuery;

import java.util.Collection;
import java.util.List;

/**
 * 岗位信息Service接口
 *
 * @author chengliang4810
 * @since 2025-06-04
 */
public interface SysPostService {

    /**
     * 根据主键查询岗位信息
     *
     * @param id 岗位信息主键
     * @return {@link SysPostVo } 岗位信息视图对象
     */
   SysPostVo queryById(Long id);

    /**
     * 查询岗位信息分页列表
     *
     * @param query 查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysPostVo }> 岗位信息分页对象
     */
    Page<SysPostVo> queryPageList(SysPostQuery query, PageQuery pageQuery);

    /**
     * 根据用户ID获取岗位选择框列表
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    List<Long> selectPostListByUserId(Long userId);

    /**
     * 根据用户ID查询其关联的岗位列表。
     *
     * @param userId 用户ID
     * @return 岗位列表
     */
    List<SysPostVo> selectPostsByUserId(Long userId);

    /**
     * 根据岗位ID集合查询正常岗位。
     */
    List<SysPostVo> selectPostByIds(Collection<Long> postIds);

    /**
     * 校验岗位名称
     *
     * @param post 岗位信息
     * @return 结果
     */
    boolean checkPostNameUnique(SysPostBo post);

    /**
     * 校验岗位编码
     *
     * @param post 岗位信息
     * @return 结果
     */
    boolean checkPostCodeUnique(SysPostBo post);

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 结果
     */
    int countUserPostById(Long postId);

    /**
     * 统计部门下的岗位数。
     */
    long countPostByDeptId(Long deptId);

   /**
     * 查询岗位信息列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysPostVo }> 岗位信息列表
     */
    List<SysPostVo> queryList(SysPostQuery query);

    /**
     * 新增岗位信息
     *
     * @param bo 岗位信息业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysPostBo bo);

    /**
     * 更新岗位信息
     *
     * @param bo 岗位信息业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysPostBo bo);

    /**
     * 批量删除岗位信息
     *
     * @param ids 岗位信息主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);
}
