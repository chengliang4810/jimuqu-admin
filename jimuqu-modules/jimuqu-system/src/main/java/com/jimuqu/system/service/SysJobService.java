package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.bo.SysJobBo;
import com.jimuqu.system.domain.query.SysJobQuery;
import com.jimuqu.system.domain.vo.SysJobVo;
import com.jimuqu.system.job.SysJobHandlerVo;

import java.util.Collection;
import java.util.List;

/**
 * 定时任务Service接口
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
public interface SysJobService {

    /**
     * 根据主键查询定时任务
     */
    SysJobVo queryById(Long id);

    /**
     * 查询定时任务分页列表
     */
    Page<SysJobVo> queryPageList(SysJobQuery query, PageQuery pageQuery);

    /**
     * 查询定时任务列表
     */
    List<SysJobVo> queryList(SysJobQuery query);

    /**
     * 查询可用处理器列表
     */
    List<SysJobHandlerVo> listHandlers();

    /**
     * 新增定时任务
     */
    Boolean insertByBo(SysJobBo bo);

    /**
     * 更新定时任务
     */
    Boolean updateByBo(SysJobBo bo);

    /**
     * 批量删除定时任务
     */
    Integer deleteByIds(Collection<Long> ids);

    /**
     * 启动任务
     */
    Boolean start(Long id);

    /**
     * 停止任务
     */
    Boolean stop(Long id);

    /**
     * 手动执行一次任务
     */
    Boolean run(Long id);
}
