package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.redis.utils.RedisUtils;
import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.domain.bo.ScheduledJobConfigBo;
import com.jimuqu.system.domain.bo.ScheduledJobDefinitionBo;
import com.jimuqu.system.domain.query.ScheduledJobLogQuery;
import com.jimuqu.system.domain.vo.ScheduledJobHandlerVo;
import com.jimuqu.system.domain.vo.ScheduledJobLogVo;
import com.jimuqu.system.domain.vo.ScheduledJobVo;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.java_cron.CronExpressionPlus;
import org.noear.java_cron.CronUtils;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.solon.core.event.AppLoadEndEvent;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.scheduling.ScheduledAnno;
import org.noear.solon.scheduling.annotation.Scheduled;
import org.noear.solon.scheduling.scheduled.JobHolder;
import org.noear.solon.scheduling.scheduled.manager.IJobManager;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jimuqu.system.service.ScheduledJobConfigService.CONCURRENT_ALLOW;
import static com.jimuqu.system.service.ScheduledJobConfigService.CONCURRENT_FORBID;
import static com.jimuqu.system.service.ScheduledJobConfigService.MAX_RETRIES;
import static com.jimuqu.system.service.ScheduledJobConfigService.MAX_RETRY_INTERVAL_MS;
import static com.jimuqu.system.service.ScheduledJobConfigService.MISFIRE_FIRE_ONCE;
import static com.jimuqu.system.service.ScheduledJobConfigService.MISFIRE_IGNORE;
import static com.jimuqu.system.service.ScheduledJobConfigService.SOURCE_DYNAMIC;
import static com.jimuqu.system.service.ScheduledJobConfigService.SOURCE_SYSTEM;

/**
 * Solon 运行时与数据库动态定时任务管理。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobService {

    /**
     * 集群任务控制消息主题。
     */
    private static final String CONTROL_TOPIC = "scheduled-job:control";

    /**
     * 动态任务新增或更新动作。
     */
    private static final String ACTION_UPSERT = "UPSERT";

    /**
     * 仅变更任务启停状态动作。
     */
    private static final String ACTION_STATE = "STATE";

    /**
     * 动态任务删除动作。
     */
    private static final String ACTION_DELETE = "DELETE";

    /**
     * Cron 调度类型。
     */
    private static final String TYPE_CRON = "CRON";

    /**
     * 固定频率调度类型。
     */
    private static final String TYPE_FIXED_RATE = "FIXED_RATE";

    /**
     * 固定延迟调度类型。
     */
    private static final String TYPE_FIXED_DELAY = "FIXED_DELAY";

    /**
     * 当前节点已运行状态。
     */
    private static final String RUNTIME_RUNNING = "RUNNING";

    /**
     * 当前节点已停止状态。
     */
    private static final String RUNTIME_STOPPED = "STOPPED";

    /**
     * 当前节点注册异常状态。
     */
    private static final String RUNTIME_ERROR = "ERROR";

    /**
     * 最大调度间隔与首次延迟。
     */
    private static final long MAX_SCHEDULE_INTERVAL_MS = TimeUnit.DAYS.toMillis(365);

    /**
     * 本节点默认允许并发提交的手动任务数量。
     */
    private static final int DEFAULT_MANUAL_MAX_CONCURRENT = 16;

    /**
     * 恢复判定使用的执行类型。
     */
    private static final List<String> RECOVERABLE_TRIGGER_TYPES =
            List.of("SCHEDULED", "RECOVERY");

    /**
     * 固定频率与 Cron 恢复基线允许的首次尝试状态。
     */
    private static final List<String> FIRST_ATTEMPT_RECOVERY_STATUSES =
            List.of("SUCCESS", "FAILED", "RETRY");

    /**
     * 固定延迟恢复基线允许的最终状态。
     */
    private static final List<String> TERMINAL_RECOVERY_STATUSES =
            List.of("SUCCESS", "FAILED");

    /**
     * Solon 任务管理器。
     */
    private final IJobManager jobManager;

    /**
     * 异步任务执行器。
     */
    private final ExecutorService executorService;

    /**
     * 状态对账调度器。
     */
    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * 持久化配置服务。
     */
    private final ScheduledJobConfigService configService;

    /**
     * 动态处理器白名单。
     */
    private final ScheduledJobHandlerRegistry handlerRegistry;

    /**
     * 执行日志 Mapper。
     */
    private final SysScheduledJobLogMapper logMapper;

    /**
     * 本节点允许并发执行的手动任务数量。
     */
    @Inject(value = "${jimuqu.scheduling.manualMaxConcurrent:16}", required = false)
    private int manualMaxConcurrent = DEFAULT_MANUAL_MAX_CONCURRENT;

    /**
     * 本节点已提交且尚未完成的手动任务数量。
     */
    private final AtomicInteger manualRunCount = new AtomicInteger();

    /**
     * 本机动态任务名称、配置代际与控制版本。
     */
    private final Map<String, DynamicRegistration> dynamicRegistrations =
            new ConcurrentHashMap<>();

    /**
     * 当前节点动态任务注册异常。
     */
    private final Map<String, RuntimeRegistrationError>
            dynamicRegistrationErrors = new ConcurrentHashMap<>();

    /**
     * Redis 主题订阅标识。
     */
    private volatile Integer topicListenerId;

    /**
     * 周期对账任务。
     */
    private volatile ScheduledFuture<?> reconcileFuture;

    /**
     * 服务是否已经销毁。
     */
    private volatile boolean destroyed;

    /**
     * 注册启动恢复、集群消息与周期对账。
     */
    @Init
    public void initialize() {
        if (manualMaxConcurrent < 1) {
            throw new IllegalStateException(
                    "jimuqu.scheduling.manualMaxConcurrent 必须大于零");
        }
        Solon.context().onEvent(AppLoadEndEvent.class, ignored -> {
            handlerRegistry.refresh();
            reconcileLocalJobs(true);
            startPeriodicReconciliation();
        });
        Solon.context().getBeanAsync(
                RedissonCacheService.class, ignored -> subscribeControlTopic());
    }

    /**
     * 释放订阅与对账任务。
     */
    @Destroy
    public synchronized void destroy() {
        destroyed = true;
        if (topicListenerId != null) {
            RedisUtils.unsubscribe(CONTROL_TOPIC, topicListenerId);
            topicListenerId = null;
        }
        if (reconcileFuture != null) {
            reconcileFuture.cancel(false);
            reconcileFuture = null;
        }
        dynamicRegistrationErrors.clear();
    }

    /**
     * 查询代码任务与在线任务。
     *
     * @return 任务列表
     */
    public synchronized List<ScheduledJobVo> list() {
        Map<String, SysScheduledJobConfig> configs = configService.listAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        SysScheduledJobConfig::getJobName, config -> config));
        List<ScheduledJobVo> result = new ArrayList<>();
        for (JobHolder job : snapshotJobs()) {
            if (dynamicRegistrations.containsKey(job.getName())) {
                continue;
            }
            SysScheduledJobConfig config = configs.get(job.getName());
            if (config == null) {
                config = configService.getOrCreate(
                        job.getName(), job.getScheduled().enable());
            } else if (SOURCE_DYNAMIC.equals(config.getJobSource())) {
                throw new ServiceException(
                        "动态任务名称与代码任务冲突: " + job.getName());
            }
            result.add(toSystemVo(job, config));
        }
        configs.values().stream()
                .filter(config -> SOURCE_DYNAMIC.equals(config.getJobSource()))
                .map(this::toDynamicVo)
                .forEach(result::add);
        return result.stream()
                .sorted(Comparator.comparing(ScheduledJobVo::getJobName))
                .toList();
    }

    /**
     * 查询在线任务可调用处理器。
     *
     * @return 处理器列表
     */
    public List<ScheduledJobHandlerVo> listHandlers() {
        return handlerRegistry.list();
    }

    /**
     * 新增在线定时任务。
     *
     * @param bo 任务定义
     */
    public synchronized void create(ScheduledJobDefinitionBo bo) {
        validateDefinition(bo);
        if (jobExists(bo.getJobName())) {
            throw new ServiceException("定时任务名称已存在: " + bo.getJobName());
        }
        SysScheduledJobConfig config = configService.createDynamic(bo);
        publishControl(new ControlMessage(
                defaultLong(config.getControlVersion(), 0L), ACTION_UPSERT,
                defaultLong(config.getConfigId(), 0L), config.getJobName()));
    }

    /**
     * 更新在线定时任务。
     *
     * @param jobName 当前任务名称
     * @param bo 任务定义
     */
    public synchronized void update(
            String jobName, ScheduledJobDefinitionBo bo) {
        validateDefinition(bo);
        SysScheduledJobConfig config = configService.updateDynamic(jobName, bo);
        publishControl(new ControlMessage(
                defaultLong(config.getControlVersion(), 0L), ACTION_UPSERT,
                defaultLong(config.getConfigId(), 0L), config.getJobName()));
    }

    /**
     * 删除在线定时任务。
     *
     * @param jobName 任务名称
     */
    public synchronized void delete(String jobName) {
        SysScheduledJobConfig deleted = configService.deleteDynamic(jobName);
        publishControl(new ControlMessage(
                defaultLong(deleted.getControlVersion(), 0L), ACTION_DELETE,
                defaultLong(deleted.getConfigId(), 0L), jobName));
    }

    /**
     * 启用任务。
     *
     * @param jobName 任务名称
     */
    public synchronized void start(String jobName) {
        SysScheduledJobConfig existing = configService.find(jobName);
        JobHolder job = getJob(jobName);
        if (existing == null && job == null) {
            throw new ServiceException("定时任务不存在: " + jobName);
        }
        boolean defaultEnabled = job != null && job.getScheduled().enable();
        SysScheduledJobConfig config = configService.updateEnabled(
                jobName, true, defaultEnabled);
        publishControl(new ControlMessage(
                defaultLong(config.getControlVersion(), 0L), ACTION_STATE,
                defaultLong(config.getConfigId(), 0L), config.getJobName()));
    }

    /**
     * 停用任务。
     *
     * @param jobName 任务名称
     */
    public synchronized void stop(String jobName) {
        SysScheduledJobConfig existing = configService.find(jobName);
        JobHolder job = getJob(jobName);
        if (existing == null && job == null) {
            throw new ServiceException("定时任务不存在: " + jobName);
        }
        boolean defaultEnabled = job != null && job.getScheduled().enable();
        SysScheduledJobConfig config = configService.updateEnabled(
                jobName, false, defaultEnabled);
        publishControl(new ControlMessage(
                defaultLong(config.getControlVersion(), 0L), ACTION_STATE,
                defaultLong(config.getConfigId(), 0L), config.getJobName()));
    }

    /**
     * 更新任务重试配置。
     *
     * @param jobName 任务名称
     * @param bo 重试配置
     */
    public synchronized void updateConfig(
            String jobName, ScheduledJobConfigBo bo) {
        JobHolder job = getJob(jobName);
        SysScheduledJobConfig existing = configService.find(jobName);
        if (existing == null && job == null) {
            throw new ServiceException("定时任务不存在: " + jobName);
        }
        SysScheduledJobConfig config = configService.updateRetry(
                jobName, job != null && job.getScheduled().enable(), bo);
        publishControl(new ControlMessage(
                defaultLong(config.getControlVersion(), 0L), ACTION_STATE,
                defaultLong(config.getConfigId(), 0L), config.getJobName()));
    }

    /**
     * 立即异步执行一次任务。
     *
     * @param jobName 任务名称
     */
    public void run(String jobName) {
        JobHolder job = getJob(jobName);
        if (job == null) {
            SysScheduledJobConfig config = configService.requireDynamic(jobName);
            job = newDynamicJobHolder(config);
        }
        if (!tryAcquireManualRun()) {
            throw new ServiceException(
                    "当前节点手动执行任务已达到并发上限，请稍后重试");
        }
        JobHolder submittedJob = job;
        try {
            executorService.execute(() -> {
                try {
                    ContextEmpty context = new ContextEmpty();
                    context.paramMap().put(
                            ScheduledJobInterceptor.MANUAL_TRIGGER, "MANUAL");
                    context.paramMap().put(
                            ScheduledJobInterceptor.MANUAL_RUN_ID,
                            UUID.randomUUID().toString());
                    submittedJob.handle(context);
                } catch (Throwable e) {
                    log.error("手动执行定时任务失败，jobName={}", jobName, e);
                } finally {
                    manualRunCount.decrementAndGet();
                }
            });
        } catch (RuntimeException submitFailure) {
            manualRunCount.decrementAndGet();
            throw new ServiceException("定时任务执行器暂不可用，请稍后重试")
                    .setDetailMessage(submitFailure.getMessage());
        }
    }

    /**
     * 尝试占用一个本节点手动任务执行名额。
     *
     * @return 是否成功占用
     */
    private boolean tryAcquireManualRun() {
        while (true) {
            int current = manualRunCount.get();
            if (current >= manualMaxConcurrent) {
                return false;
            }
            if (manualRunCount.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    /**
     * 分页查询执行日志。
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return 日志分页
     */
    public Page<ScheduledJobLogVo> queryLogPage(
            ScheduledJobLogQuery query, PageQuery pageQuery) {
        QueryChain<SysScheduledJobLog> chain = pageQuery.applyOrder(
                QueryChain.of(logMapper).forSearch(true).where(query),
                queryChain -> queryChain.orderByDesc(SysScheduledJobLog::getLogId));
        return chain.returnType(ScheduledJobLogVo.class).paging(pageQuery.build());
    }

    /**
     * 删除指定执行日志。
     *
     * @param ids 日志 ID
     * @return 删除数量
     */
    public int deleteLogs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Assert.isFalse(ids.stream().anyMatch(java.util.Objects::isNull),
                "执行日志ID不能为空");
        List<Long> requested = ids.stream().distinct().toList();
        long existing = QueryChain.of(logMapper)
                .in(SysScheduledJobLog::getLogId, requested)
                .count();
        Assert.isTrue(existing == requested.size(), "执行日志不存在");
        return logMapper.deleteByIds(requested);
    }

    /**
     * 清空执行日志。
     *
     * @return 删除数量
     */
    public int cleanLogs() {
        return logMapper.delete(
                where -> where.isNotNull(SysScheduledJobLog::getLogId));
    }

    /**
     * 校验并规范化在线任务定义。
     *
     * @param bo 任务定义
     */
    private void validateDefinition(ScheduledJobDefinitionBo bo) {
        if (bo == null) {
            throw new ServiceException("定时任务定义不能为空");
        }
        handlerRegistry.require(bo.getHandlerKey());
        String type = normalizeUpper(bo.getScheduleType(), "调度类型");
        String concurrentPolicy = normalizeUpper(
                bo.getConcurrentPolicy(), "并发策略");
        String misfirePolicy = normalizeUpper(
                bo.getMisfirePolicy(), "错过执行策略");
        if (!Set.of(TYPE_CRON, TYPE_FIXED_RATE, TYPE_FIXED_DELAY).contains(type)) {
            throw new ServiceException("不支持的调度类型: " + type);
        }
        if (!Set.of(CONCURRENT_ALLOW, CONCURRENT_FORBID)
                .contains(concurrentPolicy)) {
            throw new ServiceException("不支持的并发策略: " + concurrentPolicy);
        }
        if (!Set.of(MISFIRE_IGNORE, MISFIRE_FIRE_ONCE).contains(misfirePolicy)) {
            throw new ServiceException("不支持的错过执行策略: " + misfirePolicy);
        }
        if (bo.getInitialDelayMs() == null
                || bo.getInitialDelayMs() < 0
                || bo.getInitialDelayMs() > MAX_SCHEDULE_INTERVAL_MS) {
            throw new ServiceException(
                    "首次执行延迟必须在0到31536000000毫秒之间");
        }
        validateRetry(bo);
        if (bo.getMaxRetries() > 0
                && CONCURRENT_ALLOW.equals(concurrentPolicy)) {
            throw new ServiceException(
                    "启用失败重试时并发策略必须设置为禁止并发");
        }
        bo.setScheduleType(type);
        bo.setConcurrentPolicy(concurrentPolicy);
        bo.setMisfirePolicy(misfirePolicy);
        bo.setZone(bo.getZone() == null ? "" : bo.getZone().trim());
        if (TYPE_CRON.equals(type)) {
            bo.setInitialDelayMs(0L);
            String expression = bo.getScheduleExpression().trim();
            bo.setScheduleExpression(expression);
            validateCron(expression, bo.getZone());
        } else {
            long interval = parseInterval(bo.getScheduleExpression());
            bo.setScheduleExpression(Long.toString(interval));
            bo.setZone("");
        }
        validateProviderCapabilities(
                type, bo.getInitialDelayMs(), bo.getJobName());
    }

    /**
     * 校验当前调度器实现支持的动态调度能力。
     *
     * <p>Simple 调度器支持全部在线配置；Quartz 调度器不支持固定延迟，
     * 也不接受首次执行延迟。这里在写入数据库前给出明确错误。</p>
     *
     * @param scheduleType 调度类型
     * @param initialDelayMs 首次执行延迟毫秒数
     * @param jobName 任务名称
     */
    private void validateProviderCapabilities(
            String scheduleType, long initialDelayMs, String jobName) {
        if (!isQuartzJobManager()) {
            return;
        }
        if (TYPE_FIXED_DELAY.equals(scheduleType)) {
            throw new ServiceException(
                    "Quartz 调度器不支持固定延迟任务: " + jobName);
        }
        if (initialDelayMs > 0) {
            throw new ServiceException(
                    "Quartz 调度器不支持首次执行延迟: " + jobName);
        }
    }

    /**
     * 校验重试参数。
     *
     * @param bo 任务定义
     */
    private static void validateRetry(ScheduledJobDefinitionBo bo) {
        if (bo.getMaxRetries() == null
                || bo.getMaxRetries() < 0
                || bo.getMaxRetries() > MAX_RETRIES) {
            throw new ServiceException("最大重试次数必须在0到10之间");
        }
        if (bo.getRetryIntervalMs() == null
                || bo.getRetryIntervalMs() < 0
                || bo.getRetryIntervalMs() > MAX_RETRY_INTERVAL_MS) {
            throw new ServiceException(
                    "重试间隔必须在0到86400000毫秒之间");
        }
    }

    /**
     * 校验 Cron 表达式和时区。
     *
     * @param expression Cron 表达式
     * @param zone 时区
     */
    private static void validateCron(String expression, String zone) {
        try {
            CronExpressionPlus cron =
                    new CronExpressionPlus(CronUtils.get(expression.trim()));
            if (!zone.isEmpty()) {
                cron.setTimeZone(TimeZone.getTimeZone(ZoneId.of(zone)));
            }
            if (cron.getNextValidTimeAfter(new Date()) == null) {
                throw new ServiceException("Cron 表达式没有未来执行时间");
            }
        } catch (ServiceException validationFailure) {
            throw validationFailure;
        } catch (RuntimeException invalidCron) {
            throw new ServiceException("Cron 表达式或时区格式错误");
        }
    }

    /**
     * 解析固定间隔。
     *
     * @param expression 毫秒间隔
     * @return 间隔毫秒数
     */
    private static long parseInterval(String expression) {
        try {
            long interval = Long.parseLong(expression.trim());
            if (interval < 100L || interval > MAX_SCHEDULE_INTERVAL_MS) {
                throw new ServiceException(
                        "调度间隔必须在100到31536000000毫秒之间");
            }
            return interval;
        } catch (NumberFormatException invalidInterval) {
            throw new ServiceException("固定间隔必须是毫秒整数");
        }
    }

    /**
     * 规范化枚举字符串。
     *
     * @param value 原值
     * @param fieldName 字段名称
     * @return 大写值
     */
    private static String normalizeUpper(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(fieldName + "不能为空");
        }
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * 将代码注册任务转换为界面对象。
     *
     * @param job Solon 任务
     * @param config 持久化运行配置
     * @return 界面对象
     */
    private ScheduledJobVo toSystemVo(
            JobHolder job, SysScheduledJobConfig config) {
        Scheduled scheduled = job.getScheduled();
        boolean enabled = Boolean.TRUE.equals(config.getEnabled());
        return new ScheduledJobVo()
                .setJobName(job.getName())
                .setJobSource(SOURCE_SYSTEM)
                .setDescription(job.getSimpleName() == null
                        ? job.getName() : job.getSimpleName())
                .setScheduleType(scheduleType(scheduled))
                .setScheduleExpression(scheduleExpression(scheduled))
                .setZone(scheduled.zone())
                .setInitialDelayMs(scheduled.initialDelay())
                .setEnabled(enabled)
                .setRuntimeStatus(enabled ? "RUNNING" : "STOPPED")
                .setConcurrentPolicy(defaultString(
                        config.getConcurrentPolicy(), CONCURRENT_ALLOW))
                .setMisfirePolicy(defaultString(
                        config.getMisfirePolicy(), MISFIRE_IGNORE))
                .setMaxRetries(defaultInt(config.getMaxRetries(), 0))
                .setRetryIntervalMs(defaultLong(
                        config.getRetryIntervalMs(), 1000L))
                .setRuntimeStatus(Boolean.TRUE.equals(config.getEnabled())
                        ? RUNTIME_RUNNING : RUNTIME_STOPPED);
    }

    /**
     * 将在线任务配置转换为界面对象。
     *
     * @param config 在线任务配置
     * @return 界面对象
     */
    private ScheduledJobVo toDynamicVo(SysScheduledJobConfig config) {
        boolean enabled = Boolean.TRUE.equals(config.getEnabled());
        boolean registered = dynamicRegistrations.containsKey(
                config.getJobName()) && jobManager.jobExists(config.getJobName());
        ScheduledJobVo vo = new ScheduledJobVo()
                .setJobName(config.getJobName())
                .setJobSource(SOURCE_DYNAMIC)
                .setDescription(config.getDescription())
                .setHandlerKey(config.getHandlerKey())
                .setScheduleType(config.getScheduleType())
                .setScheduleExpression(config.getScheduleExpression())
                .setZone(config.getZone())
                .setInitialDelayMs(defaultLong(config.getInitialDelayMs(), 0L))
                .setEnabled(enabled)
                .setRuntimeStatus(!enabled ? "STOPPED"
                        : registered ? "RUNNING" : "ERROR")
                .setRuntimeError(enabled && !registered
                        ? "任务未在当前节点注册，请检查服务日志" : null)
                .setConcurrentPolicy(config.getConcurrentPolicy())
                .setMisfirePolicy(config.getMisfirePolicy())
                .setMaxRetries(defaultInt(config.getMaxRetries(), 0))
                .setRetryIntervalMs(defaultLong(
                        config.getRetryIntervalMs(), 1000L));
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return vo.setRuntimeStatus(RUNTIME_STOPPED);
        }
        RuntimeRegistrationError error =
                dynamicRegistrationErrors.get(config.getJobName());
        if (error != null && error.matches(config)) {
            return vo.setRuntimeStatus(RUNTIME_ERROR)
                    .setRuntimeError(error.message());
        }
        DynamicRegistration registration =
                dynamicRegistrations.get(config.getJobName());
        if (registration != null
                && registration.matches(config)
                && jobExists(config.getJobName())) {
            return vo.setRuntimeStatus(RUNTIME_RUNNING);
        }
        return vo.setRuntimeStatus(RUNTIME_ERROR)
                .setRuntimeError("当前节点尚未完成任务注册");
    }

    /**
     * 发布集群控制消息并立即应用到本机。
     *
     * @param control 控制消息
     */
    private void publishControl(ControlMessage control) {
        String message = control.encode();
        applyControlSafely(message);
        try {
            RedisUtils.publish(CONTROL_TOPIC, message);
        } catch (RuntimeException publishFailure) {
            log.error(
                    "发布定时任务控制消息失败，将由周期对账恢复，jobName={}, version={}",
                    control.jobName(), control.version(), publishFailure);
        }
    }

    /**
     * 订阅集群控制消息。
     */
    private synchronized void subscribeControlTopic() {
        if (!destroyed && topicListenerId == null) {
            topicListenerId = RedisUtils.subscribe(
                    CONTROL_TOPIC, String.class, this::applyControlSafely);
        }
    }

    /**
     * 安全应用集群控制消息。
     *
     * @param message 控制消息
     */
    private void applyControlSafely(String message) {
        try {
            applyControl(message);
        } catch (RuntimeException failure) {
            log.error(
                    "应用定时任务控制消息失败，将由周期对账恢复，message={}",
                    message, failure);
        }
    }

    /**
     * 应用集群任务新增、更新、启停或删除。
     *
     * @param message 控制消息
     */
    private synchronized void applyControl(String message) {
        ControlMessage control = ControlMessage.parse(message);
        if (control == null) {
            return;
        }
        SysScheduledJobConfig config = configService.find(control.jobName());
        if (ACTION_DELETE.equals(control.action())) {
            if (config != null) {
                if (control.configId() == 0L) {
                    return;
                }
                if (control.configId() > 0
                        && !java.util.Objects.equals(
                                config.getConfigId(), control.configId())) {
                    return;
                }
                if (defaultLong(config.getControlVersion(), 0L)
                        > control.version()) {
                    return;
                }
            }
            removeDynamicJob(control.jobName(), control.configId());
            clearRuntimeRegistrationError(
                    control.jobName(), control.configId());
            return;
        }
        if (config == null
                || (control.configId() > 0
                && !java.util.Objects.equals(
                        config.getConfigId(), control.configId()))
                || defaultLong(config.getControlVersion(), 0L)
                < control.version()) {
            return;
        }
        if (SOURCE_DYNAMIC.equals(config.getJobSource())) {
            if (!Boolean.TRUE.equals(config.getEnabled())) {
                removeDynamicJob(
                        config.getJobName(),
                        defaultLong(config.getConfigId(), 0L));
                clearRuntimeRegistrationError(config);
                return;
            }
            if (ACTION_STATE.equals(control.action())
                    && dynamicRegistrations.containsKey(config.getJobName())) {
                JobHolder job = getJob(config.getJobName());
                DynamicRegistration local =
                        dynamicRegistrations.get(config.getJobName());
                DynamicRegistration database =
                        DynamicRegistration.of(config);
                if (job != null
                        && local != null
                        && local.configId() == database.configId()
                        && java.util.Objects.equals(
                                local.definition(), database.definition())) {
                    dynamicRegistrations.put(
                            config.getJobName(), database);
                    applyLocalState(job, config);
                    return;
                }
            }
            registerDynamicJob(config, false);
            return;
        }
        JobHolder job = getJob(config.getJobName());
        if (job != null) {
            applyLocalState(job, config);
        }
    }

    /**
     * 对账代码任务与数据库动态任务。
     *
     * @param recoverMisfire 是否检测启动错过周期
     */
    private synchronized void reconcileLocalJobs(boolean recoverMisfire) {
        List<SysScheduledJobConfig> configs = configService.listAll();
        Map<String, SysScheduledJobConfig> configMap = new HashMap<>();
        for (SysScheduledJobConfig config : configs) {
            configMap.put(config.getJobName(), config);
            if (!SOURCE_DYNAMIC.equals(config.getJobSource())) {
                continue;
            }
            try {
                reconcileDynamicJob(config, recoverMisfire);
            } catch (RuntimeException registrationFailure) {
                removeDynamicJob(
                        config.getJobName(),
                        defaultLong(config.getConfigId(), 0L));
                log.error(
                        "动态定时任务注册失败，已停止本机运行并继续对账其他任务，jobName={}",
                        config.getJobName(), registrationFailure);
            }
        }
        for (String localDynamicName :
                List.copyOf(dynamicRegistrations.keySet())) {
            SysScheduledJobConfig config = configMap.get(localDynamicName);
            if (config == null || !SOURCE_DYNAMIC.equals(config.getJobSource())) {
                removeDynamicJob(localDynamicName);
                clearRuntimeRegistrationError(localDynamicName, 0L);
            }
        }
        for (JobHolder job : snapshotJobs()) {
            if (dynamicRegistrations.containsKey(job.getName())) {
                continue;
            }
            SysScheduledJobConfig config = configMap.get(job.getName());
            if (config == null) {
                config = configService.getOrCreate(
                        job.getName(), job.getScheduled().enable());
            } else if (SOURCE_DYNAMIC.equals(config.getJobSource())) {
                throw new ServiceException(
                        "动态任务名称与代码任务冲突: " + job.getName());
            }
            applyLocalState(job, config);
        }
    }

    /**
     * 对账一条动态任务配置。
     *
     * @param config 动态任务配置
     * @param recoverMisfire 是否检测启动错过周期
     */
    private void reconcileDynamicJob(
            SysScheduledJobConfig config, boolean recoverMisfire) {
        JobHolder sameNameJob = getJob(config.getJobName());
        if (sameNameJob != null
                && !dynamicRegistrations.containsKey(config.getJobName())) {
            throw new ServiceException(
                    "动态任务名称与代码任务冲突: " + config.getJobName());
        }
        if (Boolean.TRUE.equals(config.getEnabled())) {
            registerDynamicJob(config, recoverMisfire);
        } else {
            removeDynamicJob(
                    config.getJobName(),
                    defaultLong(config.getConfigId(), 0L));
            clearRuntimeRegistrationError(config);
        }
    }

    /**
     * 按运行期模式执行一次状态对账。
     */
    private void reconcileLocalJobs() {
        reconcileLocalJobs(false);
    }

    /**
     * 注册或按配置版本重注册在线任务。
     *
     * @param config 在线任务配置
     * @param recoverMisfire 是否检测启动错过周期
     */
    private synchronized void registerDynamicJob(
            SysScheduledJobConfig config, boolean recoverMisfire) {
        try {
            registerDynamicJobInternal(config, recoverMisfire);
            clearRuntimeRegistrationError(config);
        } catch (RuntimeException registrationFailure) {
            rememberRuntimeRegistrationError(config, registrationFailure);
            throw registrationFailure;
        }
    }

    /**
     * 注册或按配置版本重注册在线任务。
     *
     * @param config 在线任务配置
     * @param recoverMisfire 是否检测启动错过周期
     */
    private void registerDynamicJobInternal(
            SysScheduledJobConfig config, boolean recoverMisfire) {
        SysScheduledJobConfig current = configService.find(config.getJobName());
        long candidateVersion = defaultLong(config.getControlVersion(), 0L);
        if (current == null
                || !SOURCE_DYNAMIC.equals(current.getJobSource())
                || !Boolean.TRUE.equals(current.getEnabled())
                || !java.util.Objects.equals(
                        current.getConfigId(), config.getConfigId())
                || defaultLong(current.getControlVersion(), 0L) != candidateVersion) {
            removeDynamicJob(
                    config.getJobName(),
                    defaultLong(config.getConfigId(), 0L));
            return;
        }
        SysScheduledJobConfig registration = current;
        DynamicRegistration database =
                DynamicRegistration.of(registration);
        DynamicRegistration local =
                dynamicRegistrations.get(registration.getJobName());
        JobHolder currentJob = getJob(registration.getJobName());
        if (currentJob != null
                && local != null
                && local.configId() == database.configId()
                && java.util.Objects.equals(
                        local.definition(), database.definition())) {
            dynamicRegistrations.put(registration.getJobName(), database);
            applyLocalState(currentJob, registration);
            return;
        }
        removeDynamicJob(
                registration.getJobName(),
                defaultLong(registration.getConfigId(), 0L));
        handlerRegistry.require(registration.getHandlerKey());
        validateProviderCapabilities(
                registration.getScheduleType(),
                defaultLong(registration.getInitialDelayMs(), 0L),
                registration.getJobName());
        if (jobManager.jobExists(registration.getJobName())
                && !dynamicRegistrations.containsKey(
                        registration.getJobName())) {
            throw new ServiceException(
                    "动态任务名称与代码任务冲突: " + registration.getJobName());
        }
        RecoveryPlan recoveryPlan = recoverMisfire
                ? buildRecoveryPlan(registration, System.currentTimeMillis())
                : new RecoveryPlan(
                        defaultLong(registration.getInitialDelayMs(), 0L), null);
        boolean quartzFixedRate = isQuartzJobManager()
                && TYPE_FIXED_RATE.equals(registration.getScheduleType());
        long registrationDelay = quartzFixedRate
                ? 0L : recoveryPlan.effectiveInitialDelayMs();
        JobHolder job = jobManager.jobAdd(
                        registration.getJobName(),
                        buildScheduled(
                                registration,
                                registrationDelay),
                        context -> handlerRegistry.invoke(
                                registration.getHandlerKey(), context),
                        Map.of(
                                ScheduledJobInterceptor.DYNAMIC_SOURCE, "true",
                                ScheduledJobInterceptor.DYNAMIC_GENERATION,
                                ScheduledJobInterceptor.dynamicGeneration(
                                        registration)))
                    .simpleName(registration.getDescription());
        dynamicRegistrations.put(
                registration.getJobName(),
                DynamicRegistration.of(registration));
        if (recoverMisfire
                && MISFIRE_FIRE_ONCE.equals(registration.getMisfirePolicy())
                && recoveryPlan.missedCycle() != null) {
            submitRecovery(job, recoveryPlan.missedCycle());
        }
    }

    /**
     * 记录当前配置代际在本机的注册异常。
     *
     * @param config 在线任务配置
     * @param failure 注册异常
     */
    private void rememberRuntimeRegistrationError(
            SysScheduledJobConfig config, RuntimeException failure) {
        SysScheduledJobConfig current;
        try {
            current = configService.find(config.getJobName());
        } catch (RuntimeException databaseFailure) {
            log.warn(
                    "读取动态任务当前配置失败，无法保存本机注册异常，jobName={}",
                    config.getJobName(), databaseFailure);
            return;
        }
        if (current == null
                || !SOURCE_DYNAMIC.equals(current.getJobSource())
                || !Boolean.TRUE.equals(current.getEnabled())
                || !java.util.Objects.equals(
                        current.getConfigId(), config.getConfigId())
                || defaultLong(current.getControlVersion(), 0L)
                != defaultLong(config.getControlVersion(), 0L)) {
            return;
        }
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = failure.getClass().getSimpleName();
        }
        dynamicRegistrationErrors.put(
                config.getJobName(),
                new RuntimeRegistrationError(
                        defaultLong(config.getConfigId(), 0L),
                        defaultLong(config.getControlVersion(), 0L),
                        message.length() > 500
                                ? message.substring(0, 500) : message));
    }

    /**
     * 清除当前配置代际在本机的注册异常。
     *
     * @param config 在线任务配置
     */
    private void clearRuntimeRegistrationError(
            SysScheduledJobConfig config) {
        dynamicRegistrationErrors.computeIfPresent(
                config.getJobName(),
                (jobName, error) -> error.matches(config) ? null : error);
    }

    /**
     * 按配置 ID 清除当前节点的动态任务注册异常。
     *
     * @param jobName 任务名称
     * @param configId 配置 ID，零表示清除该名称的全部旧状态
     */
    private void clearRuntimeRegistrationError(
            String jobName, long configId) {
        dynamicRegistrationErrors.computeIfPresent(
                jobName,
                (ignored, error) -> configId == 0L
                        || error.configId() == configId ? null : error);
    }

    /**
     * 删除本机在线任务注册。
     *
     * @param jobName 任务名称
     */
    private synchronized void removeDynamicJob(String jobName) {
        removeDynamicJob(jobName, 0L);
    }

    /**
     * 按配置代际删除本机在线任务注册。
     *
     * @param jobName 任务名称
     * @param configId 配置代际，零表示不校验代际
     */
    private synchronized void removeDynamicJob(
            String jobName, long configId) {
        DynamicRegistration registration =
                dynamicRegistrations.get(jobName);
        if (registration == null
                || (configId > 0 && registration.configId() != configId)) {
            return;
        }
        if (jobManager.jobExists(jobName)) {
            jobManager.jobRemove(jobName);
        }
        dynamicRegistrations.remove(jobName, registration);
    }

    /**
     * 在线程安全边界内获取运行时任务。
     *
     * @param jobName 任务名称
     * @return 运行时任务
     */
    private synchronized JobHolder getJob(String jobName) {
        return jobManager.jobGet(jobName);
    }

    /**
     * 在线程安全边界内判断运行时任务是否存在。
     *
     * @param jobName 任务名称
     * @return 是否存在
     */
    private synchronized boolean jobExists(String jobName) {
        return jobManager.jobExists(jobName);
    }

    /**
     * 在线程安全边界内复制运行时任务快照。
     *
     * @return 运行时任务快照
     */
    private synchronized List<JobHolder> snapshotJobs() {
        return new ArrayList<>(jobManager.jobGetAll().values());
    }

    /**
     * 构建未注册但可走统一拦截器手动执行的在线任务。
     *
     * @param config 在线任务配置
     * @return 临时任务
     */
    private JobHolder newDynamicJobHolder(SysScheduledJobConfig config) {
        handlerRegistry.require(config.getHandlerKey());
        JobHolder job = new JobHolder(
                jobManager, config.getJobName(),
                buildScheduled(
                        config, defaultLong(config.getInitialDelayMs(), 0L)),
                context -> handlerRegistry.invoke(
                        config.getHandlerKey(), context))
                .simpleName(config.getDescription());
        job.setData(Map.of(
                ScheduledJobInterceptor.DYNAMIC_SOURCE, "true",
                ScheduledJobInterceptor.DYNAMIC_GENERATION,
                ScheduledJobInterceptor.dynamicGeneration(config)));
        return job;
    }

    /**
     * 根据持久化定义构建 Solon 调度注解。
     *
     * @param config 在线任务配置
     * @param effectiveInitialDelayMs 本次注册实际使用的首次延迟毫秒数
     * @return Solon 调度定义
     */
    private static Scheduled buildScheduled(
            SysScheduledJobConfig config, long effectiveInitialDelayMs) {
        ScheduledAnno scheduled = new ScheduledAnno()
                .name(config.getJobName())
                .enable(Boolean.TRUE.equals(config.getEnabled()))
                .initialDelay(effectiveInitialDelayMs);
        String type = config.getScheduleType();
        if (TYPE_FIXED_DELAY.equals(type)) {
            return scheduled.fixedDelay(
                    Long.parseLong(config.getScheduleExpression()));
        }
        if (TYPE_FIXED_RATE.equals(type)) {
            return scheduled.fixedRate(
                    Long.parseLong(config.getScheduleExpression()));
        }
        return scheduled.cron(config.getScheduleExpression())
                .zone(defaultString(config.getZone(), ""));
    }

    /**
     * 异步补偿一个错过的调度周期。
     *
     * @param job 运行时任务
     * @param missedCycle 错过的调度周期时间
     */
    private void submitRecovery(JobHolder job, long missedCycle) {
        executorService.execute(() -> {
            try {
                ContextEmpty context = new ContextEmpty();
                context.paramMap().put(
                        ScheduledJobInterceptor.MANUAL_TRIGGER, "RECOVERY");
                context.paramMap().put(
                        ScheduledJobInterceptor.RECOVERY_CYCLE,
                        Long.toString(missedCycle));
                job.handle(context);
            } catch (Throwable recoveryFailure) {
                log.error(
                        "恢复执行错过的定时任务失败，jobName={}, missedCycle={}",
                        job.getName(), missedCycle, recoveryFailure);
            }
        });
    }

    /**
     * 计算需要恢复的首个错过周期，供启动恢复和测试复用。
     *
     * @param config 在线任务配置
     * @param now 当前时间毫秒数
     * @return 错过周期时间，无错过时返回 null
     */
    Long findMissedCycle(SysScheduledJobConfig config, long now) {
        return buildRecoveryPlan(config, now).missedCycle();
    }

    /**
     * 计算启动恢复计划。
     *
     * <p>固定频率与固定延迟任务从上次执行时间继续计算。已经错过周期时，
     * 常规调度统一延后一个完整间隔，避免 Simple 调度器立即执行与恢复执行
     * 同时触发；是否补偿由调用方根据错过执行策略决定。</p>
     *
     * @param config 在线任务配置
     * @param now 当前时间毫秒数
     * @return 启动恢复计划
     */
    private RecoveryPlan buildRecoveryPlan(
            SysScheduledJobConfig config, long now) {
        QueryChain<SysScheduledJobLog> latestQuery = QueryChain.of(logMapper)
                .eq(SysScheduledJobLog::getJobName, config.getJobName())
                .in(SysScheduledJobLog::getTriggerType, RECOVERABLE_TRIGGER_TYPES);
        if (TYPE_FIXED_DELAY.equals(config.getScheduleType())) {
            latestQuery.in(
                    SysScheduledJobLog::getStatus,
                    TERMINAL_RECOVERY_STATUSES);
        } else {
            latestQuery
                    .eq(SysScheduledJobLog::getAttempt, 1)
                    .in(SysScheduledJobLog::getStatus,
                            FIRST_ATTEMPT_RECOVERY_STATUSES);
        }
        latestQuery
                .orderByDesc(SysScheduledJobLog::getStartTime)
                .$limit()
                .set(0, 1);
        SysScheduledJobLog latest = latestQuery.get();
        Date definitionDate = config.getUpdateTime() != null
                ? config.getUpdateTime() : config.getCreateTime();
        Date executionDate = latest == null ? null
                : TYPE_FIXED_DELAY.equals(config.getScheduleType())
                && latest.getEndTime() != null
                ? latest.getEndTime() : latest.getStartTime();
        if (executionDate != null && definitionDate != null
                && executionDate.before(definitionDate)) {
            latest = null;
            executionDate = null;
        }
        Date baselineDate = executionDate != null ? executionDate : definitionDate;
        long configuredInitialDelay =
                defaultLong(config.getInitialDelayMs(), 0L);
        if (baselineDate == null) {
            return new RecoveryPlan(configuredInitialDelay, null);
        }
        if (TYPE_CRON.equals(config.getScheduleType())) {
            CronExpressionPlus cron = new CronExpressionPlus(
                    CronUtils.get(config.getScheduleExpression()));
            if (config.getZone() != null && !config.getZone().isBlank()) {
                cron.setTimeZone(TimeZone.getTimeZone(
                        ZoneId.of(config.getZone())));
            }
            Date firstMissedCron =
                    cron.getNextValidTimeAfter(baselineDate);
            if (firstMissedCron == null
                    || firstMissedCron.getTime() > now) {
                return new RecoveryPlan(configuredInitialDelay, null);
            }
            return new RecoveryPlan(
                    configuredInitialDelay, firstMissedCron.getTime());
        }
        long interval = Long.parseLong(config.getScheduleExpression());
        long delay = latest == null ? configuredInitialDelay : interval;
        long nextExpected = addSaturated(baselineDate.getTime(), delay);
        if (nextExpected <= now) {
            long missedCycle = nextExpected;
            if (TYPE_FIXED_RATE.equals(config.getScheduleType())) {
                long elapsedIntervals = (now - nextExpected) / interval;
                missedCycle = addSaturated(
                        nextExpected, elapsedIntervals * interval);
            }
            return new RecoveryPlan(interval, missedCycle);
        }
        return new RecoveryPlan(nextExpected - now, null);
    }

    /**
     * 判断当前使用的是否为 Quartz 任务管理器。
     *
     * @return 是否使用 Quartz
     */
    boolean isQuartzJobManager() {
        return jobManager.getClass().getName().startsWith(
                "org.noear.solon.scheduling.quartz.");
    }

    /**
     * 应用代码任务启停状态。
     *
     * @param job 运行时任务
     * @param config 持久化配置
     */
    private synchronized void applyLocalState(
            JobHolder job, SysScheduledJobConfig config) {
        if (Boolean.TRUE.equals(config.getEnabled())) {
            jobManager.jobStart(job.getName(), job.getData());
        } else {
            jobManager.jobStop(job.getName());
        }
    }

    /**
     * 启动周期状态对账。
     */
    private synchronized void startPeriodicReconciliation() {
        if (destroyed || reconcileFuture != null) {
            return;
        }
        long interval = Math.max(100L,
                Solon.cfg().getLong(
                        "jimuqu.scheduling.reconcileIntervalMs", 30_000L));
        reconcileFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::reconcileSafely, interval, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * 安全执行周期状态对账。
     */
    private void reconcileSafely() {
        if (destroyed) {
            return;
        }
        try {
            reconcileLocalJobs(false);
        } catch (RuntimeException failure) {
            log.error("定时任务状态周期对账失败", failure);
        }
    }

    /**
     * 获取代码任务调度类型。
     *
     * @param scheduled Solon 调度定义
     * @return 调度类型
     */
    private static String scheduleType(Scheduled scheduled) {
        if (scheduled.fixedDelay() > 0) {
            return TYPE_FIXED_DELAY;
        }
        if (scheduled.fixedRate() > 0) {
            return TYPE_FIXED_RATE;
        }
        return TYPE_CRON;
    }

    /**
     * 获取代码任务调度表达式。
     *
     * @param scheduled Solon 调度定义
     * @return 调度表达式
     */
    private static String scheduleExpression(Scheduled scheduled) {
        if (scheduled.fixedDelay() > 0) {
            return Long.toString(scheduled.fixedDelay());
        }
        if (scheduled.fixedRate() > 0) {
            return Long.toString(scheduled.fixedRate());
        }
        return scheduled.cron();
    }

    /**
     * 空值字符串回退。
     *
     * @param value 当前值
     * @param fallback 回退值
     * @return 非空值
     */
    private static String defaultString(String value, String fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 空值整数回退。
     *
     * @param value 当前值
     * @param fallback 回退值
     * @return 非空值
     */
    private static int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 空值长整数回退。
     *
     * @param value 当前值
     * @param fallback 回退值
     * @return 非空值
     */
    private static long defaultLong(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 饱和相加，避免时间溢出。
     *
     * @param value 当前值
     * @param increment 增量
     * @return 相加结果
     */
    private static long addSaturated(long value, long increment) {
        return value > Long.MAX_VALUE - increment
                ? Long.MAX_VALUE : value + increment;
    }

    /**
     * 启动恢复计划。
     *
     * @param effectiveInitialDelayMs 本次注册实际使用的首次延迟毫秒数
     * @param missedCycle 需要补偿的错过周期时间，无需补偿时为空
     */
    private record RecoveryPlan(
            long effectiveInitialDelayMs, Long missedCycle) {
    }

    /**
     * 本机动态任务注册代际。
     *
     * @param configId 配置 ID，用于区分删除后同名重建的任务
     * @param version 当前控制版本
     * @param definition 已注册的任务定义
     */
    private record DynamicRegistration(
            long configId, long version, DynamicDefinition definition) {

        /**
         * 从持久化配置构建注册代际。
         *
         * @param config 持久化配置
         * @return 注册代际
         */
        private static DynamicRegistration of(
                SysScheduledJobConfig config) {
            return new DynamicRegistration(
                    defaultLong(config.getConfigId(), 0L),
                    defaultLong(config.getControlVersion(), 0L),
                    DynamicDefinition.of(config));
        }

        /**
         * 判断是否属于指定持久化配置代际。
         *
         * @param config 持久化配置
         * @return 配置 ID 与控制版本均相同返回 true
         */
        private boolean matches(SysScheduledJobConfig config) {
            return configId == defaultLong(config.getConfigId(), 0L)
                    && version == defaultLong(
                    config.getControlVersion(), 0L);
        }
    }

    /**
     * 当前节点动态任务注册异常。
     *
     * @param configId 配置 ID
     * @param version 控制版本
     * @param message 面向管理员的异常摘要
     */
    private record RuntimeRegistrationError(
            long configId, long version, String message) {

        /**
         * 判断异常是否属于指定持久化配置代际。
         *
         * @param config 持久化配置
         * @return 配置 ID 与控制版本均相同返回 true
         */
        private boolean matches(SysScheduledJobConfig config) {
            return configId == defaultLong(config.getConfigId(), 0L)
                    && version == defaultLong(
                    config.getControlVersion(), 0L);
        }
    }

    /**
     * 会影响运行时任务 Holder 的持久化定义。
     *
     * @param handlerKey 白名单处理器标识
     * @param scheduleType 调度类型
     * @param scheduleExpression 调度表达式
     * @param zone Cron 时区
     * @param initialDelayMs 首次执行延迟毫秒数
     * @param description 任务说明
     */
    private record DynamicDefinition(
            String handlerKey, String scheduleType, String scheduleExpression,
            String zone, long initialDelayMs, String description) {

        /**
         * 从持久化配置构建运行时定义。
         *
         * @param config 持久化配置
         * @return 运行时定义
         */
        private static DynamicDefinition of(
                SysScheduledJobConfig config) {
            return new DynamicDefinition(
                    defaultString(config.getHandlerKey(), ""),
                    defaultString(config.getScheduleType(), ""),
                    defaultString(config.getScheduleExpression(), ""),
                    defaultString(config.getZone(), ""),
                    defaultLong(config.getInitialDelayMs(), 0L),
                    defaultString(config.getDescription(), ""));
        }
    }

    /**
     * 集群控制消息。
     *
     * @param version 控制版本
     * @param action 控制动作
     * @param configId 配置 ID，用于隔离同名任务的不同代际
     * @param jobName 任务名称
     */
    private record ControlMessage(
            long version, String action, long configId, String jobName) {

        /**
         * 编码控制消息。
         *
         * @return 消息文本
         */
        private String encode() {
            return version + ":" + action + ":" + configId + ":" + jobName;
        }

        /**
         * 解析控制消息并兼容旧的版本加名称格式。
         *
         * @param message 消息文本
         * @return 控制消息，非法时返回 null
         */
        private static ControlMessage parse(String message) {
            if (message == null) {
                return null;
            }
            String[] parts = message.split(":", 4);
            try {
                long version = Long.parseLong(parts[0]);
                if (version < 0) {
                    return null;
                }
                if (parts.length == 2 && !parts[1].isBlank()) {
                    return new ControlMessage(
                            version, ACTION_UPSERT, 0L, parts[1]);
                }
                if (parts.length == 3
                        && validControlAction(parts[1])
                        && !parts[2].isBlank()) {
                    return new ControlMessage(
                            version, parts[1], 0L, parts[2]);
                }
                if (parts.length != 4
                        || !validControlAction(parts[1])
                        || parts[3].isBlank()) {
                    return null;
                }
                long configId = Long.parseLong(parts[2]);
                if (configId < 0) {
                    return null;
                }
                return new ControlMessage(
                        version, parts[1], configId, parts[3]);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                return null;
            }
        }

        /**
         * 判断控制动作是否受支持。
         *
         * @param action 控制动作
         * @return 是否受支持
         */
        private static boolean validControlAction(String action) {
            return ACTION_UPSERT.equals(action)
                    || ACTION_STATE.equals(action)
                    || ACTION_DELETE.equals(action);
        }
    }
}
