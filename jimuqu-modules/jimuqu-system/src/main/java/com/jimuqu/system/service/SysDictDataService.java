package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.bo.SysDictDataBo;
import com.jimuqu.system.domain.vo.SysDictDataVo;
import com.jimuqu.system.domain.query.SysDictDataQuery;

import java.util.Collection;
import java.util.List;

/**
 * 字典数据Service接口
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
public interface SysDictDataService {

    /**
     * 根据主键查询字典数据
     *
     * @param id 字典数据主键
     * @return {@link SysDictDataVo } 字典数据视图对象
     */
   SysDictDataVo queryById(Long id);

    /**
     * 查询字典数据分页列表
     *
     * @param query 查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysDictDataVo }> 字典数据分页对象
     */
    Page<SysDictDataVo> queryPageList(SysDictDataQuery query, PageQuery pageQuery);

   /**
     * 查询字典数据列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysDictDataVo }> 字典数据列表
     */
    List<SysDictDataVo> queryList(SysDictDataQuery query);

    /**
     * 新增字典数据
     *
     * @param bo 字典数据业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysDictDataBo bo);

    /**
     * 更新字典数据
     *
     * @param bo 字典数据业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysDictDataBo bo);

    boolean checkDictDataUnique(SysDictDataBo bo);

    /**
     * 批量删除代码生成模板信息
     *
     * @param ids 字典数据主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);


    /**
     * 按类型键查询列表
     *
     * @param dictTypeKey 字典类型键
     * @return {@link List }<{@link SysDictDataVo }>
     */
    List<SysDictDataVo> queryListByTypeKey(String dictTypeKey);
}
