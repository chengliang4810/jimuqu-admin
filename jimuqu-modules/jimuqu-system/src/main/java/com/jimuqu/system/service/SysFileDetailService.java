package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.query.SysFileDetailQuery;
import com.jimuqu.system.domain.vo.SysFileDetailVo;

import java.util.Collection;
import java.util.List;

/**
 * 文件记录Service接口
 *
 * @author chengliang4810
 * @since 2025-06-24
 */
public interface SysFileDetailService {

    /**
     * 根据主键查询文件记录
     *
     * @param id 文件记录主键
     * @return {@link SysFileDetailVo } 文件记录视图对象
     */
   SysFileDetailVo queryById(String id);

    /**
     * 查询文件记录分页列表
     *
     * @param query 查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysFileDetailVo }> 文件记录分页对象
     */
    Page<SysFileDetailVo> queryPageList(SysFileDetailQuery query, PageQuery pageQuery);

   /**
     * 查询文件记录列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysFileDetailVo }> 文件记录列表
     */
    List<SysFileDetailVo> queryList(SysFileDetailQuery query);

    /**
     * 批量删除代码生成模板信息
     *
     * @param ids 文件记录主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<String> ids);
}
