package com.jimuqu.system.task;

import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;
import org.noear.solon.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Date;

/**
 * 清理过期的定时任务执行日志。
 */
@Component
@RequiredArgsConstructor
public class ScheduledJobLogCleanupTask {

    /**
     * 执行日志保留时长。
     */
    private static final long RETENTION_MS = Duration.ofDays(30).toMillis();

    /**
     * 定时任务执行日志 Mapper。
     */
    private final SysScheduledJobLogMapper logMapper;

    /**
     * 清理超过保留时长的执行日志。
     */
    @ScheduledJobHandler(
            key = "system.scheduledJobLog.cleanExpired",
            description = "清理超过保留期限的定时任务执行日志")
    @Scheduled(name = "scheduledJobLogCleanup", cron = "0 0 3 * * ? *", enable = false)
    public void cleanExpiredLogs() {
        logMapper.delete(where -> where.lt(
                SysScheduledJobLog::getStartTime,
                new Date(System.currentTimeMillis() - RETENTION_MS)));
    }
}
