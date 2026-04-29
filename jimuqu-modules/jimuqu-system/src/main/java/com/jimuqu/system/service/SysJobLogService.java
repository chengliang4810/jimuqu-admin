package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.query.SysJobLogQuery;
import com.jimuqu.system.domain.vo.SysJobLogVo;

import java.util.Collection;
import java.util.List;

/**
 * 定时任务运行日志Service接口
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
public interface SysJobLogService {

    /**
     * 根据主键查询运行日志
     */
    SysJobLogVo queryById(Long id);

    /**
     * 查询运行日志分页列表
     */
    Page<SysJobLogVo> queryPageList(SysJobLogQuery query, PageQuery pageQuery);

    /**
     * 查询运行日志列表
     */
    List<SysJobLogVo> queryList(SysJobLogQuery query);

    /**
     * 批量删除运行日志
     */
    Integer deleteByIds(Collection<Long> ids);

    /**
     * 清空运行日志
     */
    Integer clear();
}
