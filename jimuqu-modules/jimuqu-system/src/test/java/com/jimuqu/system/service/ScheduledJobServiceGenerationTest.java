package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.BaseQuery;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.domain.vo.ScheduledJobVo;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import org.junit.jupiter.api.Test;
import org.noear.solon.scheduling.annotation.Scheduled;
import org.noear.solon.scheduling.scheduled.JobHandler;
import org.noear.solon.scheduling.scheduled.JobHolder;
import org.noear.solon.scheduling.scheduled.manager.IJobManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 动态任务运行时配置代际、乱序控制和恢复执行测试。
 */
class ScheduledJobServiceGenerationTest {

    /** 验证旧代禁用快照对账不会注销已注册的新代同名任务。 */
    @Test
    void staleDisabledReconciliationDoesNotRemoveNewGeneration() throws Exception {
        RuntimeFixture fixture = new RuntimeFixture();
        SysScheduledJobConfig current = dynamicConfig(
                22L, 220L, "generationJob", "FIXED_DELAY",
                "1000", true, "IGNORE", "test.handler");
        SysScheduledJobConfig stale = dynamicConfig(
                11L, 110L, "generationJob", "FIXED_DELAY",
                "1000", false, "IGNORE", "test.handler");
        fixture.persisted.put(current.getJobName(), current);
        register(fixture.service, current, false);
        JobHolder registered = fixture.runtimes.get(current.getJobName());
        when(fixture.configService.listAll()).thenReturn(List.of(stale));

        reconcile(fixture.service);

        assertSame(registered, fixture.runtimes.get(current.getJobName()),
                "旧代禁用快照不得注销新 configId 的同名任务");
        assertEquals(1, fixture.additions.get());
    }

    /** 验证旧代注册候选不会注销已注册的新代同名任务。 */
    @Test
    void staleRegistrationCandidateDoesNotRemoveNewGeneration() throws Exception {
        RuntimeFixture fixture = new RuntimeFixture();
        SysScheduledJobConfig current = dynamicConfig(
                42L, 420L, "candidateJob", "FIXED_DELAY",
                "1000", true, "IGNORE", "test.handler");
        SysScheduledJobConfig stale = dynamicConfig(
                31L, 310L, "candidateJob", "FIXED_DELAY",
                "1000", true, "IGNORE", "test.handler");
        fixture.persisted.put(current.getJobName(), current);
        register(fixture.service, current, false);
        JobHolder registered = fixture.runtimes.get(current.getJobName());

        register(fixture.service, stale, false);

        assertSame(registered, fixture.runtimes.get(current.getJobName()),
                "旧 configId 的注册候选不得移除当前新代任务");
        assertEquals(1, fixture.additions.get());
    }

    /** 验证 STATE 先到且 UPSERT 迟到时最终仍会重建最新任务定义。 */
    @Test
    void outOfOrderStateAndUpsertRebuildTheLatestDefinition() throws Exception {
        RuntimeFixture fixture = new RuntimeFixture();
        SysScheduledJobConfig versionOne = dynamicConfig(
                51L, 1L, "outOfOrderJob", "FIXED_DELAY",
                "1000", true, "IGNORE", "test.handler");
        fixture.persisted.put(versionOne.getJobName(), versionOne);
        register(fixture.service, versionOne, false);
        JobHolder versionOneHolder = fixture.runtimes.get(versionOne.getJobName());

        SysScheduledJobConfig versionThree = dynamicConfig(
                51L, 3L, "outOfOrderJob", "FIXED_RATE",
                "2000", true, "IGNORE", "test.handler");
        fixture.persisted.put(versionThree.getJobName(), versionThree);
        applyControl(fixture.service, "3:STATE:51:outOfOrderJob");
        applyControl(fixture.service, "2:UPSERT:51:outOfOrderJob");

        JobHolder latest = fixture.runtimes.get(versionThree.getJobName());
        assertNotSame(versionOneHolder, latest,
                "V3 STATE 先到后，迟到的 V2 UPSERT 仍必须淘汰 V1 holder");
        assertEquals(2000L, latest.getScheduled().fixedRate());
        assertEquals(0L, latest.getScheduled().fixedDelay());
    }

    /** 验证控制版本和重试参数变化不会重建相同执行定义的任务。 */
    @Test
    void reconciliationUpdatesControlVersionWithoutRebuildingHolder()
            throws Exception {
        RuntimeFixture fixture = new RuntimeFixture();
        SysScheduledJobConfig original = dynamicConfig(
                52L, 1L, "retryPolicyJob", "FIXED_DELAY",
                "1000", true, "IGNORE", "test.handler");
        fixture.persisted.put(original.getJobName(), original);
        register(fixture.service, original, false);
        JobHolder registered = fixture.runtimes.get(original.getJobName());

        SysScheduledJobConfig updated = dynamicConfig(
                52L, 2L, "retryPolicyJob", "FIXED_DELAY",
                "1000", true, "IGNORE", "test.handler");
        updated.setMaxRetries(3);
        updated.setRetryIntervalMs(500L);
        fixture.persisted.put(updated.getJobName(), updated);
        when(fixture.configService.listAll()).thenReturn(List.of(updated));

        reconcile(fixture.service);

        assertSame(registered, fixture.runtimes.get(updated.getJobName()),
                "非执行定义变化不得重建运行时任务");
        assertEquals(1, fixture.additions.get());
    }

    /** 验证单条缺失处理器的坏配置不会阻断其他动态任务对账。 */
    @Test
    void invalidPersistedTaskDoesNotBlockOtherReconciliation() throws Exception {
        RuntimeFixture fixture = new RuntimeFixture();
        SysScheduledJobConfig invalid = dynamicConfig(
                61L, 610L, "aInvalidJob", "FIXED_DELAY",
                "1000", true, "IGNORE", "missing.handler");
        SysScheduledJobConfig valid = dynamicConfig(
                62L, 620L, "bValidJob", "FIXED_RATE",
                "1000", true, "IGNORE", "test.handler");
        fixture.persisted.put(invalid.getJobName(), invalid);
        fixture.persisted.put(valid.getJobName(), valid);
        when(fixture.configService.listAll()).thenReturn(List.of(invalid, valid));
        doAnswer(invocation -> {
            if ("missing.handler".equals(invocation.getArgument(0))) {
                throw new ServiceException("测试处理器不存在");
            }
            return null;
        }).when(fixture.handlerRegistry).require(anyString());

        reconcile(fixture.service);

        assertFalse(fixture.runtimes.containsKey(invalid.getJobName()));
        assertTrue(fixture.runtimes.containsKey(valid.getJobName()),
                "单条坏配置不得阻断后续合法任务注册");
        ScheduledJobVo invalidVo = fixture.service.list().stream()
                .filter(job -> invalid.getJobName().equals(job.getJobName()))
                .findFirst()
                .orElseThrow();
        assertEquals("ERROR", invalidVo.getRuntimeStatus());
        assertEquals("测试处理器不存在",
                invalidVo.getRuntimeError());
    }

    /** 验证零延迟固定任务启动恢复只提交一次且正常调度延后完整周期。 */
    @Test
    void fixedZeroDelayFireOnceSubmitsRecoveryOnceAndDelaysNormalSchedule()
            throws Exception {
        RuntimeFixture fixture = new RuntimeFixture();
        long createdAt = System.currentTimeMillis() - 5_000L;
        SysScheduledJobConfig config = dynamicConfig(
                71L, 710L, "fireOnceJob", "FIXED_RATE",
                "1000", true, "FIRE_ONCE", "test.handler");
        config.setInitialDelayMs(0L);
        config.setCreateTime(new Date(createdAt));
        fixture.persisted.put(config.getJobName(), config);

        register(fixture.service, config, true);
        register(fixture.service, config, true);

        Scheduled scheduled = fixture.schedules.get(config.getJobName());
        assertEquals(1000L, scheduled.initialDelay(),
                "提交恢复执行后，正常 fixedRate 调度必须延后一个完整周期");
        assertEquals(1, fixture.recoveryTasks.size(),
                "同一配置代际重复对账只能提交一次恢复执行");
    }

    /** 验证 IGNORE 策略不会提交恢复执行。 */
    @Test
    void fixedZeroDelayIgnoreDoesNotRecover() throws Exception {
        RuntimeFixture fixture = new RuntimeFixture();
        SysScheduledJobConfig config = dynamicConfig(
                72L, 720L, "ignoreRecoveryJob", "FIXED_RATE",
                "1000", true, "IGNORE", "test.handler");
        config.setInitialDelayMs(0L);
        config.setCreateTime(new Date(System.currentTimeMillis() - 5_000L));
        fixture.persisted.put(config.getJobName(), config);

        register(fixture.service, config, true);

        assertEquals(1000L,
                fixture.schedules.get(config.getJobName()).initialDelay());
        assertTrue(fixture.recoveryTasks.isEmpty(),
                "IGNORE 策略不得提交任何恢复执行");
    }

    /**
     * 验证每个节点都会提交恢复尝试，由拦截器原子认领唯一执行权。
     */
    @Test
    void clusterNodesSubmitRecoveryAttemptsForAtomicClaim() throws Exception {
        RuntimeFixture firstNode = new RuntimeFixture();
        RuntimeFixture secondNode = new RuntimeFixture();
        long createdAt = System.currentTimeMillis() - 300_000L;
        SysScheduledJobConfig config = dynamicConfig(
                73L, 730L, "clusterRecoveryJob", "FIXED_RATE",
                "60000", true, "FIRE_ONCE", "test.handler");
        config.setCreateTime(new Date(createdAt));
        firstNode.persisted.put(config.getJobName(), config);
        secondNode.persisted.put(config.getJobName(), config);

        register(firstNode.service, config, true);
        register(secondNode.service, config, true);

        assertEquals(1, firstNode.recoveryTasks.size());
        assertEquals(1, secondNode.recoveryTasks.size(),
                "每个节点都必须进入拦截器原子认领，避免预写水位后宕机漏跑");

        RuntimeFixture rebuiltGeneration = new RuntimeFixture();
        SysScheduledJobConfig rebuilt = dynamicConfig(
                74L, 740L, "clusterRecoveryJob", "FIXED_RATE",
                "60000", true, "FIRE_ONCE", "test.handler");
        rebuilt.setCreateTime(new Date(createdAt));
        rebuiltGeneration.persisted.put(rebuilt.getJobName(), rebuilt);
        register(rebuiltGeneration.service, rebuilt, true);

        assertEquals(1, rebuiltGeneration.recoveryTasks.size(),
                "删除后同名重建的新 configId 必须能够独立补偿");
    }

    /**
     * 验证固定频率恢复选择最新已错过周期，而不是逐个追赶历史周期。
     */
    @Test
    void fixedRateRecoveryUsesLatestMissedCycle() {
        RuntimeFixture fixture = new RuntimeFixture();
        long createdAt = System.currentTimeMillis() - 5_500L;
        SysScheduledJobConfig config = dynamicConfig(
                75L, 750L, "latestMissedCycleJob", "FIXED_RATE",
                "1000", true, "FIRE_ONCE", "test.handler");
        config.setCreateTime(new Date(createdAt));

        Long missedCycle = fixture.service.findMissedCycle(
                config, createdAt + 5_500L);

        assertEquals(createdAt + 5_000L, missedCycle);
    }

    /** 验证 Cron 恢复使用下一合法触发时间检测首个错过周期。 */
    @Test
    void cronRecoveryUsesFirstMissedFireTime() {
        RuntimeFixture fixture = new RuntimeFixture();
        long baseline = Instant.parse(
                "2026-07-25T00:00:00Z").toEpochMilli();
        SysScheduledJobConfig config = dynamicConfig(
                76L, 760L, "cronMissedCycleJob", "CRON",
                "0 0 1 * * ? *", true, "FIRE_ONCE", "test.handler");
        config.setZone("UTC");
        config.setCreateTime(new Date(baseline));

        Long missedCycle = fixture.service.findMissedCycle(
                config, baseline + TimeUnit.HOURS.toMillis(3));

        assertEquals(
                baseline + TimeUnit.HOURS.toMillis(1),
                missedCycle);
    }

    /** 验证任务定义更新后不会使用旧定义执行日志计算补偿。 */
    @Test
    void definitionUpdateIgnoresOlderExecutionLog() {
        RuntimeFixture fixture = new RuntimeFixture();
        long now = System.currentTimeMillis();
        SysScheduledJobConfig config = dynamicConfig(
                79L, 790L, "updatedDefinitionJob", "FIXED_RATE",
                "1000", true, "FIRE_ONCE", "test.handler");
        config.setInitialDelayMs(5_000L);
        config.setCreateTime(new Date(now - 20_000L));
        config.setUpdateTime(new Date(now - 500L));
        fixture.latestLog.set(new SysScheduledJobLog()
                .setJobName(config.getJobName())
                .setStartTime(new Date(now - 10_000L))
                .setTriggerType("SCHEDULED")
                .setStatus("SUCCESS")
                .setAttempt(1));

        Long missedCycle = fixture.service.findMissedCycle(config, now);

        assertNull(missedCycle,
                "旧定义日志不得触发新定义的错过执行补偿");
    }

    /**
     * 验证 Quartz 固定频率任务以零延迟注册，并由首次触发承担补偿。
     */
    @Test
    void quartzFixedRateRegistersZeroDelayWithRecoveryFallback()
            throws Exception {
        RuntimeFixture fixture =
                new RuntimeFixture(true);
        SysScheduledJobConfig config = dynamicConfig(
                76L, 760L, "quartzFixedRateJob", "FIXED_RATE",
                "1000", true, "FIRE_ONCE", "test.handler");
        config.setCreateTime(new Date(System.currentTimeMillis() - 5_000L));
        fixture.persisted.put(config.getJobName(), config);

        register(fixture.service, config, true);

        Scheduled scheduled = fixture.schedules.get(config.getJobName());
        assertEquals(0L, scheduled.initialDelay(),
                "Quartz 不接受非零 initialDelay");
        assertEquals(1000L, scheduled.fixedRate());
        assertEquals(1, fixture.recoveryTasks.size(),
                "Quartz 必须保留独立补偿兜底，由运行时观察正常首次触发后去重");
    }

    /**
     * 验证 Quartz 首次触发缺失时由独立恢复执行一次业务。
     */
    @Test
    void quartzMissingFirstTriggerExecutesRecoveryFallback()
            throws Exception {
        RuntimeFixture fixture =
                new RuntimeFixture(true);
        SysScheduledJobConfig config = dynamicConfig(
                78L, 780L, "quartzFallbackJob", "FIXED_RATE",
                "1000", true, "FIRE_ONCE", "test.handler");
        config.setCreateTime(new Date(System.currentTimeMillis() - 5_000L));
        fixture.persisted.put(config.getJobName(), config);

        register(fixture.service, config, true);
        fixture.recoveryTasks.get(0).run();

        assertEquals(1, fixture.handlerInvocations.get(),
                "Quartz 首次触发缺失时必须由 RECOVERY 兜底执行一次");
        assertEquals(List.of("RECOVERY"), fixture.triggerTypes);
    }

    /**
     * 验证手动任务完成后会释放本节点并发名额。
     */
    @Test
    void completedManualRunReleasesLocalCapacity() throws Exception {
        RuntimeFixture fixture = new RuntimeFixture();
        SysScheduledJobConfig config = dynamicConfig(
                81L, 810L, "manualCapacityJob", "FIXED_DELAY",
                "1000", true, "IGNORE", "test.handler");
        fixture.persisted.put(config.getJobName(), config);
        register(fixture.service, config, false);
        setManualMaxConcurrent(fixture.service, 1);

        fixture.service.run(config.getJobName());
        ServiceException full = assertThrows(
                ServiceException.class,
                () -> fixture.service.run(config.getJobName()));
        assertEquals(
                "当前节点手动执行任务已达到并发上限，请稍后重试",
                full.getMessage());

        fixture.recoveryTasks.get(0).run();
        fixture.service.run(config.getJobName());
        fixture.recoveryTasks.get(1).run();

        assertEquals(2, fixture.handlerInvocations.get());
    }

    /**
     * 验证执行器拒绝提交时立即释放本节点并发名额。
     */
    @Test
    void rejectedManualRunReleasesLocalCapacity() throws Exception {
        RuntimeFixture fixture = new RuntimeFixture();
        SysScheduledJobConfig config = dynamicConfig(
                82L, 820L, "manualRejectedJob", "FIXED_DELAY",
                "1000", true, "IGNORE", "test.handler");
        fixture.persisted.put(config.getJobName(), config);
        register(fixture.service, config, false);
        setManualMaxConcurrent(fixture.service, 1);
        doThrow(new RejectedExecutionException("测试拒绝提交"))
                .when(fixture.executor).execute(any(Runnable.class));

        ServiceException first = assertThrows(
                ServiceException.class,
                () -> fixture.service.run(config.getJobName()));
        ServiceException second = assertThrows(
                ServiceException.class,
                () -> fixture.service.run(config.getJobName()));

        assertEquals("定时任务执行器暂不可用，请稍后重试", first.getMessage());
        assertEquals("定时任务执行器暂不可用，请稍后重试", second.getMessage());
    }

    /** 构造完整的动态任务持久化配置。 */
    private static SysScheduledJobConfig dynamicConfig(
            long configId, long version, String jobName,
            String scheduleType, String scheduleExpression,
            boolean enabled, String misfirePolicy, String handlerKey) {
        SysScheduledJobConfig config = new SysScheduledJobConfig()
                .setConfigId(configId)
                .setJobName(jobName)
                .setJobSource(ScheduledJobConfigService.SOURCE_DYNAMIC)
                .setDescription("运行时配置代际测试任务")
                .setHandlerKey(handlerKey)
                .setScheduleType(scheduleType)
                .setScheduleExpression(scheduleExpression)
                .setZone("")
                .setInitialDelayMs(0L)
                .setEnabled(enabled)
                .setConcurrentPolicy("FORBID")
                .setMisfirePolicy(misfirePolicy)
                .setMaxRetries(0)
                .setRetryIntervalMs(0L)
                .setControlVersion(version);
        config.setCreateBy(0L);
        config.setCreateDept(0L);
        config.setUpdateBy(0L);
        return config;
    }

    /** 反射调用动态任务注册流程。 */
    private static void register(
            ScheduledJobService service,
            SysScheduledJobConfig config,
            boolean recoverMisfire) throws Exception {
        Method method = ScheduledJobService.class.getDeclaredMethod(
                "registerDynamicJob",
                SysScheduledJobConfig.class, boolean.class);
        method.setAccessible(true);
        method.invoke(service, config, recoverMisfire);
    }

    /** 反射应用指定集群控制消息。 */
    private static void applyControl(
            ScheduledJobService service, String message) throws Exception {
        Method method = ScheduledJobService.class.getDeclaredMethod(
                "applyControl", String.class);
        method.setAccessible(true);
        method.invoke(service, message);
    }

    /** 反射执行一次本机动态任务对账。 */
    private static void reconcile(ScheduledJobService service) throws Exception {
        Method method = ScheduledJobService.class.getDeclaredMethod(
                "reconcileLocalJobs", boolean.class);
        method.setAccessible(true);
        method.invoke(service, false);
    }

    /**
     * 设置测试服务的本节点手动任务并发上限。
     *
     * @param service 被测服务
     * @param maximum 并发上限
     */
    private static void setManualMaxConcurrent(
            ScheduledJobService service, int maximum) throws Exception {
        Field field = ScheduledJobService.class.getDeclaredField(
                "manualMaxConcurrent");
        field.setAccessible(true);
        field.setInt(service, maximum);
    }

    /**
     * 可观察的 Solon 任务管理器与持久化配置测试夹具。
     */
    private static final class RuntimeFixture {

        /** 模拟 Solon 任务管理器。 */
        private final IJobManager jobManager = mock(IJobManager.class);

        /** 模拟异步执行器。 */
        private final ExecutorService executor = mock(ExecutorService.class);

        /** 模拟周期对账执行器。 */
        private final ScheduledExecutorService scheduler =
                mock(ScheduledExecutorService.class);

        /** 模拟配置服务。 */
        private final ScheduledJobConfigService configService =
                mock(ScheduledJobConfigService.class);

        /** 模拟处理器白名单。 */
        private final ScheduledJobHandlerRegistry handlerRegistry =
                mock(ScheduledJobHandlerRegistry.class);

        /** 模拟执行日志 Mapper。 */
        private final SysScheduledJobLogMapper logMapper =
                mock(SysScheduledJobLogMapper.class);

        /** 模拟查询到的最近执行日志。 */
        private final AtomicReference<SysScheduledJobLog> latestLog =
                new AtomicReference<>();

        /** 当前数据库配置快照。 */
        private final Map<String, SysScheduledJobConfig> persisted =
                new ConcurrentHashMap<>();

        /** 当前运行时任务。 */
        private final Map<String, JobHolder> runtimes =
                new ConcurrentHashMap<>();

        /** 当前运行时调度定义。 */
        private final Map<String, Scheduled> schedules =
                new ConcurrentHashMap<>();

        /** 运行时注册次数。 */
        private final AtomicInteger additions = new AtomicInteger();

        /** 已提交的恢复执行。 */
        private final List<Runnable> recoveryTasks =
                new CopyOnWriteArrayList<>();

        /** 测试处理器执行次数。 */
        private final AtomicInteger handlerInvocations =
                new AtomicInteger();

        /** 测试处理器收到的触发类型。 */
        private final List<String> triggerTypes =
                new CopyOnWriteArrayList<>();

        /** 被测运行时服务。 */
        private final ScheduledJobService service;

        /** 初始化可观察测试夹具。 */
        private RuntimeFixture() {
            this(false);
        }

        /**
         * 初始化支持 Quartz 能力模拟的测试夹具。
         *
         * @param quartzManager 是否模拟 Quartz 任务管理器
         */
        private RuntimeFixture(boolean quartzManager) {
            when(configService.find(anyString())).thenAnswer(
                    invocation -> persisted.get(invocation.getArgument(0)));
            when(jobManager.jobExists(anyString())).thenAnswer(
                    invocation -> runtimes.containsKey(invocation.getArgument(0)));
            when(jobManager.jobGet(anyString())).thenAnswer(
                    invocation -> runtimes.get(invocation.getArgument(0)));
            when(jobManager.jobGetAll()).thenAnswer(
                    ignored -> Map.copyOf(runtimes));
            when(logMapper.getEntityType()).thenReturn(
                    SysScheduledJobLog.class);
            when(logMapper.get(any(BaseQuery.class))).thenAnswer(
                    ignored -> latestLog.get());
            when(jobManager.jobAdd(
                    anyString(), any(Scheduled.class),
                    any(JobHandler.class), anyMap())).thenAnswer(invocation -> {
                String jobName = invocation.getArgument(0);
                Scheduled scheduled = invocation.getArgument(1);
                JobHandler handler = invocation.getArgument(2);
                Map<String, String> data = invocation.getArgument(3);
                JobHolder holder = new JobHolder(
                        jobManager, jobName, scheduled, handler);
                holder.setData(data);
                runtimes.put(jobName, holder);
                schedules.put(jobName, scheduled);
                additions.incrementAndGet();
                return holder;
            });
            doAnswer(invocation -> {
                String jobName = invocation.getArgument(0);
                runtimes.remove(jobName);
                schedules.remove(jobName);
                return null;
            }).when(jobManager).jobRemove(anyString());
            doAnswer(invocation -> {
                recoveryTasks.add(invocation.getArgument(0));
                return null;
            }).when(executor).execute(any(Runnable.class));
            try {
                doAnswer(invocation -> {
                    org.noear.solon.core.handle.Context context =
                            invocation.getArgument(1);
                    handlerInvocations.incrementAndGet();
                    triggerTypes.add(context.param(
                            ScheduledJobInterceptor.MANUAL_TRIGGER));
                    return null;
                }).when(handlerRegistry).invoke(
                        anyString(),
                        any(org.noear.solon.core.handle.Context.class));
            } catch (Throwable impossibleDuringStubbing) {
                throw new IllegalStateException(
                        "定时任务处理器测试桩初始化失败",
                        impossibleDuringStubbing);
            }
            service = new ScheduledJobService(
                    jobManager, executor, scheduler, configService,
                    handlerRegistry, logMapper) {

                /**
                 * 返回当前夹具模拟的调度器类型。
                 */
                @Override
                boolean isQuartzJobManager() {
                    return quartzManager;
                }
            };
        }
    }
}
