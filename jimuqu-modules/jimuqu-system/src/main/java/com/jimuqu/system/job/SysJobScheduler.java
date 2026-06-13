package com.jimuqu.system.job;

import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.excel.core.LargeExcelExportResult;
import com.jimuqu.system.domain.SysJob;
import com.jimuqu.system.domain.SysJobLog;
import com.jimuqu.system.mapper.SysJobLogMapper;
import com.jimuqu.system.mapper.SysJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final String RESULT_TYPE_EXCEL = "EXCEL";

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
            saveLog(job, LOG_SKIP, new Date(), new Date(), "上一次任务仍在运行，已跳过本次触发", null);
            return;
        }
        Date startTime = new Date();
        Integer status = LOG_SUCCESS;
        String errorMessage = null;
        JobResultAttachment resultAttachment = null;
        try {
            Object result = handlerRegistry.invoke(job);
            resultAttachment = saveResultAttachment(job, result);
        } catch (Throwable e) {
            status = LOG_FAIL;
            errorMessage = StringUtil.substring(e.getMessage() == null ? e.toString() : e.getMessage(), 0, 4000);
            log.error("定时任务执行失败 jobId={}, handlerKey={}", job.getId(), job.getHandlerKey(), e);
        } finally {
            Date endTime = new Date();
            saveLog(job, status, startTime, endTime, errorMessage, resultAttachment);
            job.setLastRunTime(endTime);
            if (isEnabled(job)) {
                job.setNextRunTime(calculateNextRunTime(job.getCronExpression()));
            }
            sysJobMapper.update(job);
            runningJobIds.remove(jobId);
        }
    }

    private JobResultAttachment saveResultAttachment(SysJob job, Object result) {
        if (result instanceof LargeExcelExportResult exportResult) {
            try {
                Path source = exportResult.getPath();
                Path targetDir = Paths.get("runtime", "job-results", String.valueOf(job.getId()));
                Files.createDirectories(targetDir);
                Path target = targetDir.resolve(System.currentTimeMillis() + "-" + sanitizeFileName(exportResult.getFileName()));
                Files.move(source, target);
                return JobResultAttachment.excel(exportResult, target);
            } catch (IOException e) {
                throw new IllegalStateException("保存定时报表导出结果失败", e);
            }
        }
        return null;
    }

    private void saveLog(SysJob job,
                         Integer status,
                         Date startTime,
                         Date endTime,
                         String errorMessage,
                         JobResultAttachment resultAttachment) {
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
        if (resultAttachment != null) {
            logEntity.setResultType(resultAttachment.resultType)
                    .setResultFileName(resultAttachment.fileName)
                    .setResultFilePath(resultAttachment.filePath)
                    .setResultContentType(resultAttachment.contentType)
                    .setResultFileSize(resultAttachment.fileSize)
                    .setResultTotalRows(resultAttachment.totalRows)
                    .setResultFileCount(resultAttachment.fileCount);
        }
        sysJobLogMapper.save(logEntity);
    }

    private boolean isEnabled(SysJob job) {
        return job != null && STATUS_ENABLED == (job.getStatus() == null ? STATUS_DISABLED : job.getStatus());
    }

    private String sanitizeFileName(String fileName) {
        return StringUtil.defaultIfBlank(fileName, "job-result").replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private record JobResultAttachment(String resultType,
                                       String fileName,
                                       String filePath,
                                       String contentType,
                                       Long fileSize,
                                       Long totalRows,
                                       Integer fileCount) {

        private static JobResultAttachment excel(LargeExcelExportResult result, Path path) throws IOException {
            return new JobResultAttachment(
                    RESULT_TYPE_EXCEL,
                    result.getFileName(),
                    path.toAbsolutePath().normalize().toString(),
                    result.getContentType(),
                    Files.size(path),
                    result.getTotalRows(),
                    result.getFileCount()
            );
        }
    }
}
