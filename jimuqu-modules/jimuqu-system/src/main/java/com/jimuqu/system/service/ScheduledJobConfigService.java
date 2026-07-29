package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.bo.ScheduledJobConfigBo;
import com.jimuqu.system.domain.bo.ScheduledJobDefinitionBo;
import com.jimuqu.system.mapper.SysScheduledJobConfigMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 定时任务持久化配置。
 */
@Component
@RequiredArgsConstructor
public class ScheduledJobConfigService {

    /**
     * 代码注册任务来源。
     */
    public static final String SOURCE_SYSTEM = "SYSTEM";

    /**
     * 在线注册任务来源。
     */
    public static final String SOURCE_DYNAMIC = "DYNAMIC";

    /**
     * 允许并发执行。
     */
    public static final String CONCURRENT_ALLOW = "ALLOW";

    /**
     * 禁止并发执行。
     */
    public static final String CONCURRENT_FORBID = "FORBID";

    /**
     * 忽略错过的执行周期。
     */
    public static final String MISFIRE_IGNORE = "IGNORE";

    /**
     * 错过周期后恢复执行一次。
     */
    public static final String MISFIRE_FIRE_ONCE = "FIRE_ONCE";

    /**
     * 最大允许重试次数。
     */
    public static final int MAX_RETRIES = 10;

    /**
     * 最大重试间隔毫秒数。
     */
    public static final long MAX_RETRY_INTERVAL_MS = 86_400_000L;

    /**
     * 乐观锁最大尝试次数。
     */
    private static final int MAX_OPTIMISTIC_RETRIES = 5;

    /**
     * 定时任务配置 Mapper。
     */
    private final SysScheduledJobConfigMapper mapper;

    /**
     * 获取或创建代码注册任务的运行配置。
     *
     * @param jobName 任务名称
     * @param defaultEnabled 代码默认启用状态
     * @return 任务配置
     */
    public SysScheduledJobConfig getOrCreate(String jobName, boolean defaultEnabled) {
        SysScheduledJobConfig config = find(jobName);
        if (config != null) {
            if (SOURCE_DYNAMIC.equals(config.getJobSource())) {
                throw new ServiceException(
                        "动态任务名称与代码任务冲突: " + jobName);
            }
            return config;
        }
        SysScheduledJobConfig created = new SysScheduledJobConfig()
                .setJobName(jobName)
                .setJobSource(SOURCE_SYSTEM)
                .setInitialDelayMs(0L)
                .setEnabled(defaultEnabled)
                .setConcurrentPolicy(CONCURRENT_ALLOW)
                .setMisfirePolicy(MISFIRE_IGNORE)
                .setMaxRetries(0)
                .setRetryIntervalMs(1000L)
                .setControlVersion(0L);
        created.setCreateDept(0L);
        created.setCreateBy(0L);
        created.setUpdateBy(0L);
        Date now = new Date();
        created.setCreateTime(now);
        created.setUpdateTime(now);
        try {
            mapper.save(created);
            return created;
        } catch (RuntimeException duplicateOrDatabaseFailure) {
            config = find(jobName);
            if (config != null) {
                if (SOURCE_DYNAMIC.equals(config.getJobSource())) {
                    throw new ServiceException(
                            "动态任务名称与代码任务冲突: " + jobName);
                }
                return config;
            }
            throw duplicateOrDatabaseFailure;
        }
    }

    /**
     * 获取全部持久化配置。
     *
     * @return 配置列表
     */
    public List<SysScheduledJobConfig> listAll() {
        return QueryChain.of(mapper)
                .orderByAsc(SysScheduledJobConfig::getJobName)
                .list();
    }

    /**
     * 获取指定任务配置。
     *
     * @param jobName 任务名称
     * @return 配置，不存在时返回 null
     */
    public SysScheduledJobConfig find(String jobName) {
        return QueryChain.of(mapper)
                .eq(SysScheduledJobConfig::getJobName, jobName)
                .get();
    }

    /**
     * 新增在线任务配置。
     *
     * @param bo 任务定义
     * @return 已新增配置
     */
    public SysScheduledJobConfig createDynamic(ScheduledJobDefinitionBo bo) {
        if (find(bo.getJobName()) != null) {
            throw new ServiceException("定时任务名称已存在: " + bo.getJobName());
        }
        long version = nextVersion(0L);
        SysScheduledJobConfig created = fromDefinition(bo)
                .setJobSource(SOURCE_DYNAMIC)
                .setEnabled(false)
                .setControlVersion(version);
        created.setCreateDept(0L);
        created.setCreateBy(0L);
        created.setUpdateBy(0L);
        try {
            mapper.save(created);
            return created;
        } catch (RuntimeException duplicateOrDatabaseFailure) {
            if (find(bo.getJobName()) != null) {
                throw new ServiceException(
                        "定时任务名称已存在: " + bo.getJobName());
            }
            throw duplicateOrDatabaseFailure;
        }
    }

    /**
     * 更新在线任务配置。
     *
     * @param jobName 当前任务名称
     * @param bo 任务定义
     * @return 已更新配置
     */
    public SysScheduledJobConfig updateDynamic(
            String jobName, ScheduledJobDefinitionBo bo) {
        if (!jobName.equals(bo.getJobName())) {
            throw new ServiceException("不允许修改定时任务唯一名称");
        }
        SysScheduledJobConfig initial = requireDynamic(jobName);
        Long expectedConfigId = initial.getConfigId();
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_RETRIES; attempt++) {
            SysScheduledJobConfig current =
                    attempt == 0 ? initial : requireDynamic(jobName);
            if (!Objects.equals(expectedConfigId, current.getConfigId())) {
                throw new ServiceException(
                        "定时任务已被删除并重建，请刷新后重试");
            }
            long currentVersion = versionOf(current);
            SysScheduledJobConfig update = fromDefinition(bo)
                    .setConfigId(current.getConfigId())
                    .setJobSource(SOURCE_DYNAMIC)
                    .setEnabled(current.getEnabled())
                    .setControlVersion(nextVersion(currentVersion));
            update.setUpdateBy(0L);
            update.setUpdateTime(new Date());
            int rows = mapper.update(update, where -> where
                    .eq(SysScheduledJobConfig::getConfigId, current.getConfigId())
                    .eq(SysScheduledJobConfig::getControlVersion, currentVersion));
            if (rows > 0) {
                update.setCreateTime(current.getCreateTime());
                return update;
            }
        }
        throw new ServiceException("定时任务已被并发修改，请重试");
    }

    /**
     * 删除在线任务配置。
     *
     * @param jobName 任务名称
     * @return 携带删除控制版本的已删除配置
     */
    public SysScheduledJobConfig deleteDynamic(String jobName) {
        SysScheduledJobConfig current = requireDynamic(jobName);
        long deleteVersion = nextVersion(versionOf(current));
        int rows = mapper.delete(where -> where
                .eq(SysScheduledJobConfig::getConfigId, current.getConfigId())
                .eq(SysScheduledJobConfig::getControlVersion, versionOf(current)));
        if (rows <= 0) {
            throw new ServiceException("定时任务已被并发修改，请重试");
        }
        return current.setControlVersion(deleteVersion);
    }

    /**
     * 获取必须存在的在线任务。
     *
     * @param jobName 任务名称
     * @return 在线任务配置
     */
    public SysScheduledJobConfig requireDynamic(String jobName) {
        SysScheduledJobConfig config = find(jobName);
        if (config == null) {
            throw new ServiceException("定时任务不存在: " + jobName);
        }
        if (!SOURCE_DYNAMIC.equals(config.getJobSource())) {
            throw new ServiceException("系统内置任务不允许修改调度定义: " + jobName);
        }
        return config;
    }

    /**
     * 更新任务启用状态。
     *
     * @param jobName 任务名称
     * @param enabled 是否启用
     * @param defaultEnabled 代码默认状态
     * @return 已更新配置
     */
    public SysScheduledJobConfig updateEnabled(String jobName, boolean enabled, boolean defaultEnabled) {
        SysScheduledJobConfig initial = find(jobName);
        Long expectedConfigId =
                initial == null ? null : initial.getConfigId();
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_RETRIES; attempt++) {
            SysScheduledJobConfig config =
                    attempt == 0 ? initial : find(jobName);
            if (config == null) {
                if (expectedConfigId != null) {
                    throw new ServiceException(
                            "定时任务已被删除，请刷新后重试");
                }
                config = getOrCreate(jobName, defaultEnabled);
                expectedConfigId = config.getConfigId();
            } else if (expectedConfigId != null
                    && !Objects.equals(
                            expectedConfigId, config.getConfigId())) {
                throw new ServiceException(
                        "定时任务已被删除并重建，请刷新后重试");
            }
            SysScheduledJobConfig current = config;
            long currentVersion = versionOf(current);
            long nextVersion = nextVersion(currentVersion);
            SysScheduledJobConfig update = new SysScheduledJobConfig()
                    .setConfigId(current.getConfigId())
                    .setEnabled(enabled)
                    .setControlVersion(nextVersion);
            update.setUpdateBy(0L);
            update.setUpdateTime(current.getUpdateTime());
            int rows = mapper.update(update, where -> where
                    .eq(SysScheduledJobConfig::getConfigId, current.getConfigId())
                    .eq(SysScheduledJobConfig::getControlVersion, currentVersion));
            if (rows > 0) {
                return current.setEnabled(enabled).setControlVersion(nextVersion);
            }
        }
        throw new ServiceException("定时任务状态已被并发修改，请重试");
    }

    /**
     * 更新任务重试配置。
     *
     * @param jobName 任务名称
     * @param defaultEnabled 代码默认状态
     * @param bo 重试配置
     * @return 已更新配置
     */
    public SysScheduledJobConfig updateRetry(
            String jobName, boolean defaultEnabled, ScheduledJobConfigBo bo) {
        if (bo.getMaxRetries() < 0 || bo.getMaxRetries() > MAX_RETRIES) {
            throw new ServiceException("最大重试次数必须在0到10之间");
        }
        if (bo.getRetryIntervalMs() < 0 || bo.getRetryIntervalMs() > MAX_RETRY_INTERVAL_MS) {
            throw new ServiceException("重试间隔必须在0到86400000毫秒之间");
        }
        SysScheduledJobConfig initial = find(jobName);
        Long expectedConfigId =
                initial == null ? null : initial.getConfigId();
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_RETRIES; attempt++) {
            SysScheduledJobConfig config =
                    attempt == 0 ? initial : find(jobName);
            if (config == null) {
                if (expectedConfigId != null) {
                    throw new ServiceException(
                            "定时任务已被删除，请刷新后重试");
                }
                config = getOrCreate(jobName, defaultEnabled);
                expectedConfigId = config.getConfigId();
            } else if (expectedConfigId != null
                    && !Objects.equals(
                            expectedConfigId, config.getConfigId())) {
                throw new ServiceException(
                        "定时任务已被删除并重建，请刷新后重试");
            }
            SysScheduledJobConfig current = config;
            long currentVersion = versionOf(current);
            long nextVersion = nextVersion(currentVersion);
            SysScheduledJobConfig update = new SysScheduledJobConfig()
                    .setConfigId(current.getConfigId())
                    .setMaxRetries(bo.getMaxRetries())
                    .setRetryIntervalMs(bo.getRetryIntervalMs())
                    .setControlVersion(nextVersion);
            if (bo.getMaxRetries() > 0) {
                update.setConcurrentPolicy(CONCURRENT_FORBID);
            }
            update.setUpdateBy(0L);
            update.setUpdateTime(current.getUpdateTime());
            int rows = mapper.update(update, where -> where
                    .eq(SysScheduledJobConfig::getConfigId, current.getConfigId())
                    .eq(SysScheduledJobConfig::getControlVersion, currentVersion));
            if (rows > 0) {
                current
                        .setMaxRetries(bo.getMaxRetries())
                        .setRetryIntervalMs(bo.getRetryIntervalMs())
                        .setControlVersion(nextVersion);
                if (bo.getMaxRetries() > 0) {
                    current.setConcurrentPolicy(CONCURRENT_FORBID);
                }
                return current;
            }
        }
        throw new ServiceException("定时任务已被并发修改，请重试");
    }

    /**
     * 从请求构建动态任务实体。
     *
     * @param bo 任务定义
     * @return 任务实体
     */
    private static SysScheduledJobConfig fromDefinition(ScheduledJobDefinitionBo bo) {
        return new SysScheduledJobConfig()
                .setJobName(bo.getJobName())
                .setDescription(bo.getDescription().trim())
                .setHandlerKey(bo.getHandlerKey())
                .setScheduleType(bo.getScheduleType())
                .setScheduleExpression(bo.getScheduleExpression().trim())
                .setZone(bo.getZone() == null ? "" : bo.getZone().trim())
                .setInitialDelayMs(bo.getInitialDelayMs())
                .setConcurrentPolicy(bo.getConcurrentPolicy())
                .setMisfirePolicy(bo.getMisfirePolicy())
                .setMaxRetries(bo.getMaxRetries())
                .setRetryIntervalMs(bo.getRetryIntervalMs());
    }

    /**
     * 读取控制版本。
     *
     * @param config 任务配置
     * @return 控制版本
     */
    private static long versionOf(SysScheduledJobConfig config) {
        return config.getControlVersion() == null ? 0L : config.getControlVersion();
    }

    /**
     * 生成单调递增控制版本。
     *
     * @param currentVersion 当前版本
     * @return 新版本
     */
    private static long nextVersion(long currentVersion) {
        return Math.max(System.currentTimeMillis(), currentVersion + 1L);
    }
}
