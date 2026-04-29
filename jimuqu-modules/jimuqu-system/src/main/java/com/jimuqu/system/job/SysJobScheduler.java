package com.jimuqu.system.job;

import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.system.domain.SysJob;
import com.jimuqu.system.domain.SysJobLog;
import com.jimuqu.system.mapper.SysJobLogMapper;
import com.jimuqu.system.mapper.SysJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 进程内轻量任务调度器
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysJobScheduler {

    public static final int STATUS_ENABLED = 0;
    public static final int STATUS_DISABLED = 1;
    public static final int LOG_SUCCESS = 0;
    public static final int LOG_FAIL = 1;
    public static final int LOG_SKIP = 2;

    private final ScheduledExecutorService scheduledExecutorService;
    private final SysJobMapper sysJobMapper;
    private final SysJobLogMapper sysJobLogMapper;
    private final SysJobHandlerRegistry handlerRegistry;
    private final Map<Long, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    private final Set<Long> runningJobIds = ConcurrentHashMap.newKeySet();

    /**
     * 启动时恢复启用任务。
     */
    public void restoreEnabledJobs() {
        List<SysJob> jobs = sysJobMapper.listEnabled();
        for (SysJob job : jobs) {
            schedule(job);
        }
        log.info("启用定时任务恢复完成，共恢复 {} 个", jobs.size());
    }

    /**
     * 注册或刷新任务调度。
     */
    public void schedule(SysJob job) {
        if (job == null || job.getId() == null) {
            return;
        }
        cancel(job.getId());
        if (!isEnabled(job)) {
            return;
        }
        Date nextRunTime = calculateNextRunTime(job.getCronExpression());
        job.setNextRunTime(nextRunTime);
        sysJobMapper.update(job);
        long delayMs = Math.max(0, nextRunTime.getTime() - System.currentTimeMillis());
        ScheduledFuture<?> future = scheduledExecutorService.schedule(() -> trigger(job.getId()), delayMs, TimeUnit.MILLISECONDS);
        scheduledFutures.put(job.getId(), future);
    }

    /**
     * 停止任务调度。
     */
    public void cancel(Long jobId) {
        if (jobId == null) {
            return;
        }
        ScheduledFuture<?> future = scheduledFutures.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 手动执行一次。
     */
    public void runNow(Long jobId) {
        scheduledExecutorService.execute(() -> execute(jobId, true));
    }

    /**
     * 计算下一次运行时间。
     */
    public Date calculateNextRunTime(String cronExpression) {
        LocalDateTime nextTime = new SysJobCronExpression(cronExpression).nextTimeAfter(LocalDateTime.now());
        return Date.from(nextTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private void trigger(Long jobId) {
        try {
            execute(jobId, false);
        } finally {
            SysJob latest = sysJobMapper.getById(jobId);
            if (isEnabled(latest)) {
                schedule(latest);
            } else {
                cancel(jobId);
            }
        }
    }

    private void execute(Long jobId, boolean manual) {
        SysJob job = sysJobMapper.getById(jobId);
        if (job == null) {
            cancel(jobId);
            return;
        }
        if (!manual && !isEnabled(job)) {
            return;
        }
        if (!Boolean.TRUE.equals(job.getAllowConcurrent()) && !runningJobIds.add(jobId)) {
            saveLog(job, LOG_SKIP, new Date(), new Date(), "上一次任务仍在运行，已跳过本次触发");
            return;
        }
        Date startTime = new Date();
        Integer status = LOG_SUCCESS;
        String errorMessage = null;
        try {
            handlerRegistry.invoke(job);
        } catch (Throwable e) {
            status = LOG_FAIL;
            errorMessage = StringUtil.substring(e.getMessage() == null ? e.toString() : e.getMessage(), 0, 4000);
            log.error("定时任务执行失败 jobId={}, handlerKey={}", job.getId(), job.getHandlerKey(), e);
        } finally {
            Date endTime = new Date();
            saveLog(job, status, startTime, endTime, errorMessage);
            job.setLastRunTime(endTime);
            if (isEnabled(job)) {
                job.setNextRunTime(calculateNextRunTime(job.getCronExpression()));
            }
            sysJobMapper.update(job);
            runningJobIds.remove(jobId);
        }
    }

    private void saveLog(SysJob job, Integer status, Date startTime, Date endTime, String errorMessage) {
        SysJobLog logEntity = new SysJobLog()
                .setJobId(job.getId())
                .setJobName(job.getJobName())
                .setJobGroup(job.getJobGroup())
                .setHandlerKey(job.getHandlerKey())
                .setHandlerParam(job.getHandlerParam())
                .setStatus(status)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setDurationMs(endTime.getTime() - startTime.getTime())
                .setErrorMessage(errorMessage);
        sysJobLogMapper.save(logEntity);
    }

    private boolean isEnabled(SysJob job) {
        return job != null && STATUS_ENABLED == (job.getStatus() == null ? STATUS_DISABLED : job.getStatus());
    }
}
