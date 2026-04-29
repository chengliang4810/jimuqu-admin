package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysJobLog;
import com.jimuqu.system.domain.query.SysJobLogQuery;
import com.jimuqu.system.domain.vo.SysJobLogVo;
import com.jimuqu.system.mapper.SysJobLogMapper;
import com.jimuqu.system.service.SysJobLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;

/**
 * 定时任务运行日志Service业务层处理
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysJobLogServiceImpl implements SysJobLogService {

    private final SysJobLogMapper sysJobLogMapper;

    @Override
    public SysJobLogVo queryById(Long id) {
        return sysJobLogMapper.getVoById(id);
    }

    @Override
    public Page<SysJobLogVo> queryPageList(SysJobLogQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysJobLogVo.class)
                .paging(pageQuery.build());
    }

    @Override
    public List<SysJobLogVo> queryList(SysJobLogQuery query) {
        return buildQueryChain(query)
                .returnType(SysJobLogVo.class)
                .list();
    }

    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysJobLogMapper.deleteByIds(ids);
    }

    @Override
    public Integer clear() {
        return sysJobLogMapper.delete(where -> where.isNotNull(SysJobLog::getId));
    }

    private QueryChain<SysJobLog> buildQueryChain(SysJobLogQuery query) {
        return QueryChain.of(sysJobLogMapper)
                .forSearch(true)
                .where(query);
    }
}
