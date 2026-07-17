package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.bo.SysConfigBo;
import com.jimuqu.system.domain.vo.SysConfigVo;
import com.jimuqu.system.domain.query.SysConfigQuery;

import java.util.Collection;
import java.util.List;

/**
 * 参数配置Service接口
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
public interface SysConfigService {

    /**
     * 根据主键查询参数配置
     *
     * @param id 参数配置主键
     * @return {@link SysConfigVo } 参数配置视图对象
     */
   SysConfigVo queryById(Long id);

    /**
     * 查询参数配置分页列表
     *
     * @param query 查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysConfigVo }> 参数配置分页对象
     */
    Page<SysConfigVo> queryPageList(SysConfigQuery query, PageQuery pageQuery);

   /**
     * 查询参数配置列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysConfigVo }> 参数配置列表
     */
    List<SysConfigVo> queryList(SysConfigQuery query);

    /**
     * 新增参数配置
     *
     * @param bo 参数配置业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysConfigBo bo);

    /**
     * 更新参数配置
     *
     * @param bo 参数配置业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysConfigBo bo);

    Boolean updateByKey(SysConfigBo bo);

    boolean checkConfigKeyUnique(SysConfigBo bo);

    /**
     * 批量删除代码生成模板信息
     *
     * @param ids 参数配置主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);
}
