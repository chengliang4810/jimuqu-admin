package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.bo.SysDictTypeBo;
import com.jimuqu.system.domain.vo.SysDictTypeVo;
import com.jimuqu.system.domain.query.SysDictTypeQuery;

import java.util.Collection;
import java.util.List;

/**
 * 字典类型Service接口
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
public interface SysDictTypeService {

    /**
     * 根据主键查询字典类型
     *
     * @param id 字典类型主键
     * @return {@link SysDictTypeVo } 字典类型视图对象
     */
   SysDictTypeVo queryById(Long id);

    /**
     * 查询字典类型分页列表
     *
     * @param query 查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysDictTypeVo }> 字典类型分页对象
     */
    Page<SysDictTypeVo> queryPageList(SysDictTypeQuery query, PageQuery pageQuery);

   /**
     * 查询字典类型列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysDictTypeVo }> 字典类型列表
     */
    List<SysDictTypeVo> queryList(SysDictTypeQuery query);

    /**
     * 新增字典类型
     *
     * @param bo 字典类型业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysDictTypeBo bo);

    /**
     * 更新字典类型
     *
     * @param bo 字典类型业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysDictTypeBo bo);

    boolean checkDictKeyUnique(SysDictTypeBo bo);

    void resetDictCache();

    /**
     * 批量删除字典类型
     *
     * @param ids 字典类型主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);
}
