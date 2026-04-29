package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysJob;
import com.jimuqu.system.domain.bo.SysJobBo;
import com.jimuqu.system.domain.query.SysJobQuery;
import com.jimuqu.system.domain.vo.SysJobVo;
import com.jimuqu.system.job.SysJobCronExpression;
import com.jimuqu.system.job.SysJobHandlerRegistry;
import com.jimuqu.system.job.SysJobHandlerVo;
import com.jimuqu.system.job.SysJobScheduler;
import com.jimuqu.system.mapper.SysJobMapper;
import com.jimuqu.system.service.SysJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 定时任务Service业务层处理
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysJobServiceImpl implements SysJobService {

    private final SysJobMapper sysJobMapper;
    private final SysJobScheduler sysJobScheduler;
    private final SysJobHandlerRegistry handlerRegistry;

    @Override
    public SysJobVo queryById(Long id) {
        return sysJobMapper.getVoById(id);
    }

    @Override
    public Page<SysJobVo> queryPageList(SysJobQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysJobVo.class)
                .paging(pageQuery.build());
    }

    @Override
    public List<SysJobVo> queryList(SysJobQuery query) {
        return buildQueryChain(query)
                .returnType(SysJobVo.class)
                .list();
    }

    @Override
    public List<SysJobHandlerVo> listHandlers() {
        return handlerRegistry.listHandlers();
    }

    @Override
    public Boolean insertByBo(SysJobBo bo) {
        validateJob(bo);
        SysJob sysJob = MapstructUtil.convert(bo, SysJob.class);
        fillDefaultValue(sysJob);
        boolean flag = sysJobMapper.save(sysJob) > 0;
        bo.setId(sysJob.getId());
        if (flag && isEnabled(sysJob)) {
            sysJobScheduler.schedule(sysJob);
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(SysJobBo bo) {
        validateJob(bo);
        SysJob oldJob = sysJobMapper.getById(bo.getId());
        Assert.notNull(oldJob, "定时任务不存在");
        SysJob sysJob = MapstructUtil.convert(bo, SysJob.class);
        fillDefaultValue(sysJob);
        sysJob.setLastRunTime(oldJob.getLastRunTime());
        if (!isEnabled(sysJob)) {
            sysJob.setNextRunTime(null);
        }
        boolean flag = sysJobMapper.update(sysJob) > 0;
        if (flag) {
            if (isEnabled(sysJob)) {
                sysJobScheduler.schedule(sysJob);
            } else {
                sysJobScheduler.cancel(sysJob.getId());
            }
        }
        return flag;
    }

    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        if (ids != null) {
            ids.forEach(sysJobScheduler::cancel);
        }
        return sysJobMapper.deleteByIds(ids);
    }

    @Override
    public Boolean start(Long id) {
        SysJob sysJob = sysJobMapper.getById(id);
        Assert.notNull(sysJob, "定时任务不存在");
        validateJob(sysJob);
        sysJob.setStatus(SysJobScheduler.STATUS_ENABLED);
        boolean flag = sysJobMapper.update(sysJob) > 0;
        if (flag) {
            sysJobScheduler.schedule(sysJob);
        }
        return flag;
    }

    @Override
    public Boolean stop(Long id) {
        SysJob sysJob = sysJobMapper.getById(id);
        Assert.notNull(sysJob, "定时任务不存在");
        sysJob.setStatus(SysJobScheduler.STATUS_DISABLED);
        sysJob.setNextRunTime(null);
        boolean flag = sysJobMapper.update(sysJob) > 0;
        if (flag) {
            sysJobScheduler.cancel(id);
        }
        return flag;
    }

    @Override
    public Boolean run(Long id) {
        SysJob sysJob = sysJobMapper.getById(id);
        Assert.notNull(sysJob, "定时任务不存在");
        validateJob(sysJob);
        sysJobScheduler.runNow(id);
        return true;
    }

    private QueryChain<SysJob> buildQueryChain(SysJobQuery query) {
        return QueryChain.of(sysJobMapper)
                .forSearch(true)
                .where(query);
    }

    private void validateJob(SysJobBo bo) {
        SysJob sysJob = MapstructUtil.convert(bo, SysJob.class);
        validateJob(sysJob);
    }

    private void validateJob(SysJob job) {
        Assert.isTrue(StringUtil.isNotBlank(job.getHandlerKey()), "处理器标识不能为空");
        Assert.isTrue(handlerRegistry.contains(job.getHandlerKey()), "处理器未注册或未标注白名单注解");
        new SysJobCronExpression(job.getCronExpression());
    }

    private void fillDefaultValue(SysJob sysJob) {
        if (StringUtil.isBlank(sysJob.getJobGroup())) {
            sysJob.setJobGroup("DEFAULT");
        }
        if (sysJob.getAllowConcurrent() == null) {
            sysJob.setAllowConcurrent(false);
        }
        if (sysJob.getStatus() == null) {
            sysJob.setStatus(SysJobScheduler.STATUS_DISABLED);
        }
        if (isEnabled(sysJob)) {
            Date nextRunTime = sysJobScheduler.calculateNextRunTime(sysJob.getCronExpression());
            sysJob.setNextRunTime(nextRunTime);
        }
    }

    private boolean isEnabled(SysJob sysJob) {
        return sysJob != null && SysJobScheduler.STATUS_ENABLED == (sysJob.getStatus() == null ? SysJobScheduler.STATUS_DISABLED : sysJob.getStatus());
    }
}
