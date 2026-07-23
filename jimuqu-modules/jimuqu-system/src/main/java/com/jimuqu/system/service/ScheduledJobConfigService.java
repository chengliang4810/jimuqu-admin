package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.bo.ScheduledJobConfigBo;
import com.jimuqu.system.mapper.SysScheduledJobConfigMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

/**
 * 定时任务持久化配置。
 */
@Component
@RequiredArgsConstructor
public class ScheduledJobConfigService {

    private static final int MAX_RETRIES = 10;
    private static final long MAX_RETRY_INTERVAL_MS = 86_400_000L;
    private static final int MAX_OPTIMISTIC_RETRIES = 5;

    private final SysScheduledJobConfigMapper mapper;

    public SysScheduledJobConfig getOrCreate(String jobName, boolean defaultEnabled) {
        SysScheduledJobConfig config = find(jobName);
        if (config != null) {
            return config;
        }
        SysScheduledJobConfig created = new SysScheduledJobConfig()
                .setJobName(jobName)
                .setEnabled(defaultEnabled)
                .setMaxRetries(0)
                .setRetryIntervalMs(1000L)
                .setControlVersion(0L);
        created.setCreateDept(0L);
        created.setCreateBy(0L);
        created.setUpdateBy(0L);
        try {
            mapper.save(created);
            return created;
        } catch (RuntimeException duplicateOrDatabaseFailure) {
            config = find(jobName);
            if (config != null) {
                return config;
            }
            throw duplicateOrDatabaseFailure;
        }
    }

    public SysScheduledJobConfig updateEnabled(String jobName, boolean enabled, boolean defaultEnabled) {
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_RETRIES; attempt++) {
            SysScheduledJobConfig config = getOrCreate(jobName, defaultEnabled);
            long currentVersion = config.getControlVersion() == null
                    ? 0L : config.getControlVersion();
            long nextVersion = Math.max(System.currentTimeMillis(), currentVersion + 1L);
            SysScheduledJobConfig update = new SysScheduledJobConfig()
                    .setConfigId(config.getConfigId())
                    .setEnabled(enabled)
                    .setControlVersion(nextVersion);
            update.setUpdateBy(0L);
            int rows = mapper.update(update, where -> where
                    .eq(SysScheduledJobConfig::getConfigId, config.getConfigId())
                    .eq(SysScheduledJobConfig::getControlVersion, currentVersion));
            if (rows > 0) {
                return config.setEnabled(enabled).setControlVersion(nextVersion);
            }
        }
        throw new ServiceException("定时任务状态已被并发修改，请重试");
    }

    public SysScheduledJobConfig updateRetry(
            String jobName, boolean defaultEnabled, ScheduledJobConfigBo bo) {
        if (bo.getMaxRetries() < 0 || bo.getMaxRetries() > MAX_RETRIES) {
            throw new ServiceException("最大重试次数必须在0到10之间");
        }
        if (bo.getRetryIntervalMs() < 0 || bo.getRetryIntervalMs() > MAX_RETRY_INTERVAL_MS) {
            throw new ServiceException("重试间隔必须在0到86400000毫秒之间");
        }
        SysScheduledJobConfig config = getOrCreate(jobName, defaultEnabled);
        SysScheduledJobConfig update = new SysScheduledJobConfig()
                .setConfigId(config.getConfigId())
                .setMaxRetries(bo.getMaxRetries())
                .setRetryIntervalMs(bo.getRetryIntervalMs());
        update.setUpdateBy(0L);
        if (mapper.update(update) <= 0) {
            throw new ServiceException("更新定时任务重试配置失败");
        }
        return config.setMaxRetries(bo.getMaxRetries()).setRetryIntervalMs(bo.getRetryIntervalMs());
    }

    private SysScheduledJobConfig find(String jobName) {
        return QueryChain.of(mapper)
                .eq(SysScheduledJobConfig::getJobName, jobName)
                .get();
    }
}
