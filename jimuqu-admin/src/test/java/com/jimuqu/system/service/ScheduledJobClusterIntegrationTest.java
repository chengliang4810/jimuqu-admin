package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.Application;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.redis.utils.RedisUtils;
import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.domain.bo.ScheduledJobDefinitionBo;
import com.jimuqu.system.mapper.SysScheduledJobConfigMapper;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import com.jimuqu.test.support.ManagedSchedulingTestJob;
import com.jimuqu.test.support.ScheduledJobClusterProbeController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.core.util.RunUtil;
import org.noear.solon.scheduling.scheduled.JobHolder;
import org.noear.solon.scheduling.scheduled.manager.IJobManager;
import org.noear.solon.test.SolonTest;
import org.redisson.api.RScoredSortedSet;
import org.redisson.client.codec.StringCodec;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 一个额外 Application JVM 与测试 JVM 共享 MySQL/Redis 时的真实双节点调度集群契约。
 */
@SolonTest(
        value = Application.class,
        env = "test",
        properties = {
                "jimuqu.scheduling.reconcileIntervalMs=100",
                "jimuqu.scheduling.claimLeaseMs=1500"
        },
        debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ScheduledJobClusterIntegrationTest {

    /** 测试计划结束后统一清理 Solon 全局延迟调度器。 */
    @RegisterExtension
    static final TestPlanShutdown TEST_PLAN_SHUTDOWN = new TestPlanShutdown();

    private static final String JOB_NAME = ManagedSchedulingTestJob.JOB_NAME;
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration STATE_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CLUSTER_EXECUTION_TIMEOUT = Duration.ofSeconds(30);
    /** 集成测试认领租约，需覆盖共享 CI 运行器的短时调度抖动。 */
    private static final long CLAIM_LEASE_MS = 1_500L;
    /** 至少跨越两个心跳周期后再验证存活 owner 的续租结果。 */
    private static final long HEARTBEAT_OBSERVATION_MS = CLAIM_LEASE_MS + 700L;
    /** owner 宕机后等待完整租约窗口过去，再验证其他节点接管。 */
    private static final long TAKEOVER_WAIT_MS = CLAIM_LEASE_MS + 300L;
    private static final int PORT_BIND_ATTEMPTS = 5;

    /**
     * 集群测试子 JVM 的初始堆，避免并发启动时预留过多系统提交内存。
     */
    private static final String CHILD_JVM_INITIAL_HEAP = "-Xms32m";

    /**
     * 集群测试子 JVM 的最大堆，探针应用无需继承宿主机的大内存默认值。
     */
    private static final String CHILD_JVM_MAX_HEAP = "-Xmx256m";

    /**
     * 集群测试子 JVM 使用串行收集器，减少并发测试进程的本地线程和内存开销。
     */
    private static final String CHILD_JVM_GC = "-XX:+UseSerialGC";

    /** 当前测试 JVM 的任务运行服务。 */
    @Inject
    private ScheduledJobService jobService;
    /** 集群共享的任务配置服务。 */
    @Inject
    private ScheduledJobConfigService configService;
    /** 执行日志数据访问组件。 */
    @Inject
    private SysScheduledJobLogMapper logMapper;
    /** 任务配置数据访问组件。 */
    @Inject
    private SysScheduledJobConfigMapper configMapper;
    /** 当前测试 JVM 的 Solon 任务管理器。 */
    @Inject
    private IJobManager jobManager;

    /** 当前测试持有且需要统一释放的远端节点。 */
    private final List<ClusterNode> nodes = new ArrayList<>();
    /** 集群探针日志与临时类路径文件目录。 */
    private Path artifactDirectory;

    @BeforeAll
    void startCluster() throws Exception {
        artifactDirectory = Path.of("target", "scheduler-cluster-" + ProcessHandle.current().pid())
                .toAbsolutePath();
        Files.createDirectories(artifactDirectory);
        configService.updateEnabled(JOB_NAME, false, false);
        jobManager.jobStop(JOB_NAME);
        clearExecutionEvidence();

        Path classpathJar = createClasspathJar(artifactDirectory.resolve("classpath.jar"));
        try {
            nodes.add(startNode("slow-reconcile", 600_000L, classpathJar));
            await("子节点必须按数据库初始状态停止",
                    () -> nodes.stream().noneMatch(node -> node.state().started()), STATE_TIMEOUT);
            Thread.sleep(500L);
        } catch (Throwable failure) {
            stopNodes();
            throw failure;
        }
    }

    @AfterAll
    void stopCluster() {
        try {
            jobService.stop(JOB_NAME);
        } catch (RuntimeException ignored) {
            // 测试失败后的清理不覆盖首个失败。
        }
        stopNodes();
        clearExecutionEvidence();
    }

    /** 验证真实双节点的控制通知、周期对账、互斥执行与故障接管。 */
    @Test
    void coordinatesControlReconciliationAndMutualExclusionAcrossTwoJvms() throws Exception {
        ClusterNode slowReconcile = nodes.get(0);

        jobService.start(JOB_NAME);
        await("Pub/Sub 必须启动两个 JVM 的任务",
                () -> mainStarted() && slowReconcile.state().started(), STATE_TIMEOUT);

        jobService.stop(JOB_NAME);
        await("Pub/Sub 必须停止两个 JVM 的任务",
                () -> !mainStarted() && !slowReconcile.state().started(), STATE_TIMEOUT);

        jobService.start(JOB_NAME);
        await("对账测试前两个节点必须启动",
                () -> mainStarted() && slowReconcile.state().started(), STATE_TIMEOUT);
        configService.updateEnabled(JOB_NAME, false, false);
        await("未发布消息时测试 JVM 必须依靠快速周期对账停止",
                () -> !mainStarted(), STATE_TIMEOUT);
        assertTrue(slowReconcile.state().started(),
                "十分钟对账节点不得在测试窗口内自行停止，避免把周期对账误判为 Pub/Sub");

        jobService.start(JOB_NAME);
        await("集群互斥测试前两个节点必须恢复",
                () -> mainStarted() && slowReconcile.state().started(), STATE_TIMEOUT);
        verifySingleClusterExecution(JOB_NAME, List.of(slowReconcile));
    }

    /** 验证在线任务在集群中的完整注册、恢复和删除生命周期。 */
    @Test
    void synchronizesDynamicJobsAndRestoresThemOnLateNodeStartup() throws Exception {
        String jobName = "clusterDynamicJob" + ProcessHandle.current().pid();
        ScheduledJobDefinitionBo created = dynamicDefinition(
                jobName, "FIXED_DELAY", "600000");
        ClusterNode lateNode = null;
        try {
            jobService.create(created);
            jobService.start(jobName);
            await("新增在线任务必须同步注册到全部现有节点",
                    () -> jobManager.jobGet(jobName) != null
                            && nodes.stream().allMatch(node -> {
                                NodeState state = node.state(jobName);
                                return state.registered() && state.started();
                            }), STATE_TIMEOUT);

            ScheduledJobDefinitionBo updated = dynamicDefinition(
                    jobName, "FIXED_RATE", "600000");
            updated.setDescription("已更新的集群动态任务");
            jobService.update(jobName, updated);
            await("更新在线任务必须在全部节点重建调度定义",
                    () -> scheduleMatches(jobManager.jobGet(jobName), "FIXED_RATE", "600000")
                            && nodes.stream().allMatch(node -> {
                                NodeState state = node.state(jobName);
                                return "FIXED_RATE".equals(state.scheduleType())
                                        && "600000".equals(state.scheduleExpression())
                                        && state.initialDelayMs() == 3_600_000L;
                            }), STATE_TIMEOUT);

            jobService.stop(jobName);
            await("停用在线任务必须从全部节点注销运行时调度",
                    () -> jobManager.jobGet(jobName) == null
                            && nodes.stream().noneMatch(
                            node -> node.state(jobName).registered()), STATE_TIMEOUT);

            jobService.start(jobName);
            await("重新启用在线任务必须同步注册到全部节点",
                    () -> jobManager.jobGet(jobName) != null
                            && nodes.stream().allMatch(
                            node -> node.state(jobName).started()), STATE_TIMEOUT);

            lateNode = startNode(
                    "late-dynamic-restore", 100L,
                    artifactDirectory.resolve("classpath.jar"));
            ClusterNode restoredNode = lateNode;
            await("晚启动节点必须从数据库恢复在线任务",
                    () -> {
                        NodeState state = restoredNode.state(jobName);
                        return state.registered() && state.started()
                                && "FIXED_RATE".equals(state.scheduleType())
                                && "600000".equals(state.scheduleExpression())
                                && state.initialDelayMs() > 3_500_000L;
                    }, STATE_TIMEOUT);

            List<ClusterNode> participants = new ArrayList<>(nodes);
            participants.add(restoredNode);
            verifySingleClusterExecution(jobName, participants);

            jobService.delete(jobName);
            await("删除在线任务必须从全部节点移除并删除数据库配置",
                    () -> configService.find(jobName) == null
                            && jobManager.jobGet(jobName) == null
                            && !restoredNode.state(jobName).registered()
                            && nodes.stream().noneMatch(
                            node -> node.state(jobName).registered()), STATE_TIMEOUT);
        } finally {
            if (lateNode != null) {
                lateNode.close();
            }
            if (configService.find(jobName) != null) {
                try {
                    jobService.delete(jobName);
                } catch (RuntimeException ignored) {
                    // 测试失败后的清理不得覆盖首个失败。
                }
            }
            clearExecutionEvidence(jobName);
        }
    }

    /** 验证禁用的零延迟在线任务在创建和节点恢复时都不会抢跑。 */
    @Test
    void keepsDisabledZeroDelayDynamicJobsUnregisteredAcrossRecovery() throws Exception {
        String jobName = "disabledClusterDynamicJob" + ProcessHandle.current().pid();
        ScheduledJobDefinitionBo disabled = dynamicDefinition(
                jobName, "FIXED_RATE", "100");
        disabled.setInitialDelayMs(0L);
        int before = totalExecutions(nodes);
        ClusterNode lateNode = null;
        try {
            jobService.create(disabled);
            await("禁用在线任务不得注册到任何现有节点",
                    () -> jobManager.jobGet(jobName) == null
                            && nodes.stream().noneMatch(
                            node -> node.state(jobName).registered()), STATE_TIMEOUT);
            Thread.sleep(350L);
            assertEquals(before, totalExecutions(nodes),
                    "禁用的零延迟 fixedRate 任务在创建后不得抢跑");

            lateNode = startNode(
                    "late-disabled-restore", 100L,
                    artifactDirectory.resolve("classpath.jar"));
            NodeState restored = lateNode.state(jobName);
            assertFalse(restored.registered(),
                    "晚启动节点不得恢复注册数据库中的禁用任务");
            assertEquals(0, restored.executions(),
                    "晚启动节点恢复禁用任务时不得发生首次执行");
            Thread.sleep(350L);
            assertEquals(0, lateNode.state(jobName).executions(),
                    "禁用的零延迟 fixedRate 任务在恢复后不得抢跑");
        } finally {
            if (lateNode != null) {
                lateNode.close();
            }
            if (configService.find(jobName) != null) {
                try {
                    jobService.delete(jobName);
                } catch (RuntimeException ignored) {
                    // 测试失败后的清理不得覆盖首个失败。
                }
            }
            clearExecutionEvidence(jobName);
        }
    }

    /** 验证错峰启动节点对同一配置代际的历史周期只补偿一次。 */
    @Test
    void staggeredStartupRecoversOneMissedCycleOnlyOnce() throws Exception {
        String jobName = "staggeredRecoveryJob" + ProcessHandle.current().pid();
        long intervalMs = 600_000L;
        long baseline = System.currentTimeMillis() - intervalMs - 5_000L;
        long missedCycle = baseline + intervalMs;
        ScheduledJobDefinitionBo definition = dynamicDefinition(
                jobName, "FIXED_RATE", Long.toString(intervalMs));
        definition.setInitialDelayMs(intervalMs);
        definition.setMisfirePolicy("FIRE_ONCE");
        ClusterNode firstNode = null;
        ClusterNode secondNode = null;
        SysScheduledJobConfig config = null;
        try {
            jobService.create(definition);
            jobService.start(jobName);
            config = configService.requireDynamic(jobName);
            Date definitionTime = new Date(baseline - 1_000L);
            SysScheduledJobConfig historicalDefinition =
                    new SysScheduledJobConfig()
                            .setConfigId(config.getConfigId());
            historicalDefinition.setCreateTime(definitionTime);
            historicalDefinition.setUpdateTime(definitionTime);
            assertEquals(1, configMapper.update(historicalDefinition),
                    "恢复测试必须回填同一配置代际的历史定义时间");
            config = configService.requireDynamic(jobName);
            await("恢复测试任务必须先同步到已有节点",
                    () -> jobManager.jobGet(jobName) != null
                            && nodes.stream().allMatch(
                            node -> node.state(jobName).registered()), STATE_TIMEOUT);
            clearExecutionEvidence(jobName);
            saveHistoricalSuccess(jobName, baseline);

            firstNode = startNode(
                    "staggered-recovery-first", 100L,
                    artifactDirectory.resolve("classpath.jar"));
            ClusterNode claimedNode = firstNode;
            await("首个晚启动节点必须补偿历史周期",
                    () -> claimedNode.state(jobName).executions() == 1,
                    STATE_TIMEOUT);
            assertEquals(1, firstNode.state(jobName).executions(),
                    "首个节点只能执行一次 FIRE_ONCE 补偿");
            firstNode.close();
            firstNode = null;

            logMapper.delete(where -> where
                    .eq(SysScheduledJobLog::getJobName, jobName)
                    .eq(SysScheduledJobLog::getTriggerType, "RECOVERY"));
            secondNode = startNode(
                    "staggered-recovery-second", 100L,
                    artifactDirectory.resolve("classpath.jar"));
            ClusterNode staleSnapshotNode = secondNode;
            await("第二个晚启动节点必须完成任务恢复注册",
                    () -> staleSnapshotNode.state(jobName).registered(),
                    STATE_TIMEOUT);
            Thread.sleep(500L);

            assertEquals(0, secondNode.state(jobName).executions(),
                    "补偿日志不可见时，配置代际水位仍必须阻止第二节点重复补偿");
        } finally {
            if (secondNode != null) {
                secondNode.close();
            }
            if (firstNode != null) {
                firstNode.close();
            }
            if (configService.find(jobName) != null) {
                try {
                    jobService.delete(jobName);
                } catch (RuntimeException ignored) {
                    // 测试失败后的清理不得覆盖首个失败。
                }
            }
            clearExecutionEvidence(jobName);
            clearRecoveryEvidence(jobName, config, missedCycle);
        }
    }

    /** 验证认领节点宕机后，其他节点会在租约到期后接管同一周期。 */
    @Test
    void reclaimsExpiredExecutionAfterClaimingNodeCrashes() throws Exception {
        String jobName = "leaseTakeoverJob" + ProcessHandle.current().pid();
        ScheduledJobDefinitionBo definition = dynamicDefinition(
                jobName, "FIXED_RATE", "600000");
        definition.setConcurrentPolicy("ALLOW");
        ClusterNode claimingNode = null;
        ClusterNode takeoverNode = nodes.get(0);
        ExecutorService fireExecutor = Executors.newSingleThreadExecutor();
        SysScheduledJobConfig config = null;
        try {
            jobService.create(definition);
            jobService.start(jobName);
            config = configService.requireDynamic(jobName);
            SysScheduledJobConfig claimedConfig = config;
            claimingNode = startNode(
                    "lease-owner", 100L,
                    artifactDirectory.resolve("classpath.jar"));
            claimingNode.mode("BLOCKING");

            ClusterNode owner = claimingNode;
            Future<?> blockedFire = fireExecutor.submit(() -> {
                owner.fire(jobName);
                return null;
            });
            String pendingKey = generationKey(jobName, config) + ":pending";
            RScoredSortedSet<String> pending = RedisUtils.getClient()
                    .getScoredSortedSet(pendingKey, StringCodec.INSTANCE);
            await("认领节点进入处理器前必须写入 PENDING 租约",
                    () -> pending.size() == 1,
                    STATE_TIMEOUT);
            await("owner 必须进入阻塞处理器后再验证心跳续租",
                    () -> owner.state(jobName).executions() == 1,
                    STATE_TIMEOUT);
            String cycleId = pending.first();
            Double initialLeaseUntil = pending.getScore(cycleId);
            assertFalse(blockedFire.isDone(),
                    "故障注入前 owner 必须仍阻塞在业务处理器中");

            Thread.sleep(HEARTBEAT_OBSERVATION_MS);
            Double renewedLeaseUntil = pending.getScore(cycleId);
            assertTrue(initialLeaseUntil != null && renewedLeaseUntil != null
                            && renewedLeaseUntil > initialLeaseUntil,
                    "存活节点执行超过初始租约后必须持续刷新 PENDING 租约");
            takeoverNode.fire(jobName);
            assertEquals(0, takeoverNode.state(jobName).executions(),
                    "owner 心跳仍存活时，其他节点不得接管同一周期");

            claimingNode.crash();
            await("强制终止节点后阻塞请求必须结束",
                    blockedFire::isDone, STATE_TIMEOUT);
            Thread.sleep(TAKEOVER_WAIT_MS);

            takeoverNode.fire(jobName);
            ClusterNode survivor = takeoverNode;
            await("租约到期后存活节点必须接管并完成原周期",
                    () -> survivor.state(jobName).executions() == 1,
                    STATE_TIMEOUT);
            await("租约接管必须完成日志与 Redis 终态持久化",
                    () -> QueryChain.of(logMapper)
                            .eq(SysScheduledJobLog::getJobName, jobName)
                            .eq(SysScheduledJobLog::getExecutionId,
                                    "scheduled:" + cycleId)
                            .eq(SysScheduledJobLog::getStatus, "SUCCESS")
                            .count() == 1L
                            && pending.size() == 0
                            && String.valueOf(RedisUtils.getClient()
                            .getMap(generationKey(jobName, claimedConfig) + ":states",
                                    StringCodec.INSTANCE)
                            .get(cycleId)).startsWith("COMPLETED|"),
                    STATE_TIMEOUT);

            List<SysScheduledJobLog> successLogs = QueryChain.of(logMapper)
                    .eq(SysScheduledJobLog::getJobName, jobName)
                    .eq(SysScheduledJobLog::getExecutionId,
                            "scheduled:" + cycleId)
                    .eq(SysScheduledJobLog::getStatus, "SUCCESS")
                    .list();
            assertEquals(1, successLogs.size(),
                    "被接管周期只能产生一条成功执行日志");
            assertEquals(0, pending.size(),
                    "成功接管后必须移除 PENDING 认领");
            assertTrue(String.valueOf(RedisUtils.getClient()
                            .getMap(generationKey(jobName, config) + ":states",
                                    StringCodec.INSTANCE)
                            .get(cycleId)).startsWith("COMPLETED|"),
                    "接管成功后同一周期必须进入 COMPLETED 状态");
        } finally {
            fireExecutor.shutdownNow();
            assertTrue(fireExecutor.awaitTermination(5, TimeUnit.SECONDS),
                    "租约故障注入线程池未退出");
            if (claimingNode != null) {
                claimingNode.close();
            }
            if (configService.find(jobName) != null) {
                try {
                    jobService.delete(jobName);
                } catch (RuntimeException ignored) {
                    // 测试失败后的清理不得覆盖首个失败。
                }
            }
            clearExecutionEvidence(jobName);
            clearGenerationEvidence(jobName, config);
        }
    }

    /** 验证旧配置代际的延迟删除消息不会注销同名重建任务。 */
    @Test
    void ignoresDelayedDeleteFromPreviousSameNameGenerationAcrossCluster() throws Exception {
        String jobName = "recreatedClusterDynamicJob" + ProcessHandle.current().pid();
        ScheduledJobDefinitionBo definition = dynamicDefinition(
                jobName, "FIXED_DELAY", "600000");
        try {
            jobService.create(definition);
            jobService.start(jobName);
            await("旧代在线任务必须先注册到全部节点",
                    () -> jobManager.jobGet(jobName) != null
                            && nodes.stream().allMatch(
                            node -> node.state(jobName).registered()), STATE_TIMEOUT);

            SysScheduledJobConfig deleted = configService.deleteDynamic(jobName);
            String delayedDelete = deleted.getControlVersion()
                    + ":DELETE:" + deleted.getConfigId() + ":" + jobName;
            RedisUtils.publish("scheduled-job:control", delayedDelete);
            await("旧代删除消息必须先移除旧任务注册",
                    () -> jobManager.jobGet(jobName) == null
                            && nodes.stream().noneMatch(
                            node -> node.state(jobName).registered()), STATE_TIMEOUT);

            jobService.create(dynamicDefinition(
                    jobName, "FIXED_DELAY", "600000"));
            jobService.start(jobName);
            SysScheduledJobConfig recreated = configService.find(jobName);
            assertNotEquals(deleted.getConfigId(), recreated.getConfigId(),
                    "同名重建任务必须分配新的配置代际");
            await("同名重建任务必须注册到全部节点",
                    () -> jobManager.jobGet(jobName) != null
                            && nodes.stream().allMatch(
                            node -> node.state(jobName).registered()), STATE_TIMEOUT);

            RedisUtils.publish("scheduled-job:control", delayedDelete);
            Thread.sleep(500L);
            assertEquals(recreated.getConfigId(),
                    configService.find(jobName).getConfigId(),
                    "延迟旧删除消息不得删除新代配置");
            assertTrue(jobManager.jobGet(jobName) != null,
                    "延迟旧删除消息不得注销测试 JVM 的新代任务");
            assertTrue(nodes.get(0).state(jobName).registered(),
                    "延迟旧删除消息不得注销慢对账节点的新代任务");
        } finally {
            if (configService.find(jobName) != null) {
                try {
                    jobService.delete(jobName);
                } catch (RuntimeException ignored) {
                    // 测试失败后的清理不得覆盖首个失败。
                }
            }
            clearExecutionEvidence(jobName);
        }
    }

    /** 验证本机应用失败不会阻止真实远端 JVM 接收控制消息并注册任务。 */
    @Test
    void localApplyFailureStillPublishesControlMessage() throws Exception {
        String jobName = "publishAfterLocalFailure" + ProcessHandle.current().pid();
        ScheduledJobDefinitionBo definition = dynamicDefinition(
                jobName, "FIXED_RATE", "600000");
        int listenerId = -1;
        try {
            SysScheduledJobConfig config =
                    configService.createDynamic(definition);
            ClusterNode remoteNode = nodes.get(0);
            NodeState remoteBefore = remoteNode.state(jobName);
            assertNotEquals(ProcessHandle.current().pid(), remoteBefore.pid(),
                    "控制消息容错必须由独立远端 JVM 验证");
            assertFalse(remoteBefore.registered(),
                    "控制消息发布前，慢对账远端 JVM 不得提前注册任务");
            IJobManager localManager = mock(IJobManager.class);
            ScheduledJobConfigService localConfig =
                    mock(ScheduledJobConfigService.class);
            ScheduledJobHandlerRegistry localRegistry =
                    mock(ScheduledJobHandlerRegistry.class);
            AtomicReference<SysScheduledJobConfig> currentConfig =
                    new AtomicReference<>(config);
            AtomicReference<String> expected = new AtomicReference<>();
            when(localConfig.find(jobName))
                    .thenAnswer(ignored -> currentConfig.get());
            when(localConfig.updateEnabled(jobName, true, false))
                    .thenAnswer(ignored -> {
                        SysScheduledJobConfig enabled =
                                configService.updateEnabled(
                                        jobName, true, false);
                        currentConfig.set(enabled);
                        expected.set(enabled.getControlVersion() + ":STATE:"
                                + enabled.getConfigId() + ":" + jobName);
                        return enabled;
                    });
            AtomicInteger localApplications = new AtomicInteger();
            doAnswer(ignored -> {
                localApplications.incrementAndGet();
                throw new ServiceException("测试本机应用失败");
            }).when(localRegistry).require(definition.getHandlerKey());
            ScheduledJobService localService = new ScheduledJobService(
                    localManager, mock(ExecutorService.class),
                    mock(ScheduledExecutorService.class), localConfig,
                    localRegistry, mock(SysScheduledJobLogMapper.class));
            CountDownLatch received = new CountDownLatch(1);
            AtomicReference<String> remoteMessage = new AtomicReference<>();
            listenerId = RedisUtils.subscribe(
                    "scheduled-job:control", String.class, message -> {
                        if (message.equals(expected.get())) {
                            remoteMessage.set(message);
                            received.countDown();
                        }
                    });
            localService.start(jobName);
            assertEquals(1, localApplications.get(),
                    "启用返回前必须尝试本机应用，失败由安全边界记录并交给对账恢复");
            assertTrue(received.await(STATE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
                    "本机应用失败后仍必须通过 Redis 发布给远端节点");
            assertEquals(expected.get(), remoteMessage.get());
            await("真实远端 JVM 必须收到控制消息并注册共享数据库中的任务",
                    () -> {
                        NodeState state = remoteNode.state(jobName);
                        return state.registered() && state.started()
                                && "FIXED_RATE".equals(state.scheduleType())
                                && "600000".equals(state.scheduleExpression());
                    }, STATE_TIMEOUT);
        } finally {
            if (listenerId >= 0) {
                RedisUtils.unsubscribe("scheduled-job:control", listenerId);
            }
            if (configService.find(jobName) != null) {
                try {
                    jobService.delete(jobName);
                } catch (RuntimeException ignored) {
                    // 测试失败后的清理不得覆盖首个失败。
                }
            }
            clearExecutionEvidence(jobName);
        }
    }

    /** 统计主测试 JVM 与指定子节点中的处理器执行总数。 */
    private static int totalExecutions(List<ClusterNode> participants) {
        return ManagedSchedulingTestJob.executions()
                + participants.stream()
                .mapToInt(node -> node.state().executions())
                .sum();
    }

    /** 验证指定任务在主 JVM 与所有子节点的同周期集群互斥。 */
    private void verifySingleClusterExecution(
            String jobName, List<ClusterNode> participants) throws Exception {
        clearExecutionEvidence(jobName);
        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.SUCCESS);
        int before = ManagedSchedulingTestJob.executions()
                + participants.stream()
                .mapToInt(node -> node.state(jobName).executions())
                .sum();

        int participantCount = participants.size() + 1;
        CountDownLatch ready = new CountDownLatch(participantCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(participantCount);
        try {
            List<Callable<Void>> calls = new ArrayList<>();
            calls.add(() -> runTogether(
                    ready, start, () -> fireMain(jobName)));
            participants.forEach(node -> calls.add(() -> runTogether(
                    ready, start, () -> node.fire(jobName))));
            List<Future<Void>> results = calls.stream().map(executor::submit).toList();
            assertTrue(ready.await(5, TimeUnit.SECONDS), "集群节点未同时准备好任务执行");
            start.countDown();
            for (Future<Void> result : results) {
                result.get(CLUSTER_EXECUTION_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "集群测试线程池未退出");
        }

        int after = ManagedSchedulingTestJob.executions()
                + participants.stream()
                .mapToInt(node -> node.state(jobName).executions())
                .sum();
        assertEquals(1, after - before, "同一调度周期整个集群只能有一个节点执行原始任务");

        List<SysScheduledJobLog> logs = QueryChain.of(logMapper)
                .eq(SysScheduledJobLog::getJobName, jobName)
                .eq(SysScheduledJobLog::getTriggerType, "SCHEDULED")
                .list();
        assertEquals(1, logs.size(), "正常集群落选不应产生冗余执行日志");
        assertEquals(1L, logs.stream().filter(log -> "SUCCESS".equals(log.getStatus())).count());
        assertEquals(0L,
                logs.stream().filter(log -> "SKIPPED".equals(log.getStatus())).count());
        assertEquals(1L,
                logs.stream().map(SysScheduledJobLog::getInstanceId).distinct().count(),
                "执行日志只记录实际取得周期执行权的实例");
    }

    private Void runTogether(CountDownLatch ready, CountDownLatch start,
                             CheckedRunnable action) throws Exception {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS), "等待集群同步执行超时");
        action.run();
        return null;
    }

    /** 在测试主 JVM 触发指定任务。 */
    private void fireMain(String jobName) throws Exception {
        JobHolder job = jobManager.jobGet(jobName);
        assertTrue(job != null, "测试 JVM 未注册集群测试任务");
        try {
            job.handle(new ContextEmpty());
        } catch (Throwable failure) {
            if (failure instanceof Exception exception) {
                throw exception;
            }
            throw new AssertionError("测试 JVM 执行任务失败", failure);
        }
    }

    private boolean mainStarted() {
        return ScheduledJobClusterProbeController.isJobStarted(jobManager);
    }

    private void clearExecutionEvidence() {
        clearExecutionEvidence(JOB_NAME);
    }

    /** 清除指定任务的日志与集群周期标记。 */
    private void clearExecutionEvidence(String jobName) {
        if (logMapper != null) {
            logMapper.delete(where -> where.eq(SysScheduledJobLog::getJobName, jobName));
        }
        if (Solon.app() != null) {
            String prefix = Solon.cfg().get("jimuqu.cache.keyHeader", "jimuqu");
            String separator = prefix.endsWith(":") ? "" : ":";
            RedisUtils.getClient().getBucket(
                    prefix + separator + "scheduled-job:{"
                            + jobName + "}:scheduled:marker",
                    StringCodec.INSTANCE).delete();
        }
    }

    /** 写入用于模拟停机窗口的历史成功执行日志。 */
    private void saveHistoricalSuccess(String jobName, long startedAt) {
        Date executedAt = new Date(startedAt);
        SysScheduledJobLog log = new SysScheduledJobLog()
                .setJobName(jobName)
                .setExecutionId("historical:" + startedAt)
                .setStatus("SUCCESS")
                .setTriggerType("SCHEDULED")
                .setAttempt(1)
                .setInstanceId("staggered-startup-fixture")
                .setStartTime(executedAt)
                .setEndTime(executedAt)
                .setDurationMs(0L);
        log.setCreateDept(0L);
        log.setCreateBy(0L);
        log.setUpdateBy(0L);
        logMapper.save(log);
    }

    /** 清除指定任务代际的恢复水位和恢复周期标记。 */
    private static void clearRecoveryEvidence(
            String jobName, SysScheduledJobConfig config, long missedCycle) {
        if (config == null || Solon.app() == null) {
            return;
        }
        String prefix = Solon.cfg().get("jimuqu.cache.keyHeader", "jimuqu");
        String separator = prefix.endsWith(":") ? "" : ":";
        String keyBase = prefix + separator + "scheduled-job:{" + jobName + "}";
        RedisUtils.getClient().getBucket(
                keyBase + ":recovery:" + missedCycle + ":marker",
                StringCodec.INSTANCE).delete();
        RedisUtils.getClient().getBucket(
                keyBase + ":recovery-watermark:"
                        + config.getConfigId() + ":"
                        + ScheduledJobInterceptor.definitionId(config),
                StringCodec.INSTANCE).delete();
    }

    /** 构建指定任务配置代际的 Redis 键前缀。 */
    private static String generationKey(
            String jobName, SysScheduledJobConfig config) {
        String prefix = Solon.cfg().get("jimuqu.cache.keyHeader", "jimuqu");
        String separator = prefix.endsWith(":") ? "" : ":";
        return prefix + separator + "scheduled-job:{" + jobName
                + "}:generation:" + config.getConfigId() + ":"
                + ScheduledJobInterceptor.definitionId(config);
    }

    /** 清除指定任务配置代际的租约、状态和完成记录。 */
    private static void clearGenerationEvidence(
            String jobName, SysScheduledJobConfig config) {
        if (config == null || Solon.app() == null) {
            return;
        }
        String generationKey = generationKey(jobName, config);
        RedisUtils.getClient().getScoredSortedSet(
                generationKey + ":pending", StringCodec.INSTANCE).delete();
        RedisUtils.getClient().getMap(
                generationKey + ":states", StringCodec.INSTANCE).delete();
        RedisUtils.getClient().getScoredSortedSet(
                generationKey + ":completed", StringCodec.INSTANCE).delete();
        RedisUtils.getClient().getBucket(
                generationKey + ":anchor", StringCodec.INSTANCE).delete();
    }

    /** 构造集群测试使用的已启用动态任务定义。 */
    private static ScheduledJobDefinitionBo dynamicDefinition(
            String jobName, String scheduleType, String scheduleExpression) {
        ScheduledJobDefinitionBo definition = new ScheduledJobDefinitionBo();
        definition.setJobName(jobName);
        definition.setDescription("集群动态任务");
        definition.setHandlerKey(ManagedSchedulingTestJob.HANDLER_KEY);
        definition.setScheduleType(scheduleType);
        definition.setScheduleExpression(scheduleExpression);
        definition.setZone("");
        definition.setInitialDelayMs(3_600_000L);
        definition.setConcurrentPolicy("FORBID");
        definition.setMisfirePolicy("IGNORE");
        definition.setMaxRetries(0);
        definition.setRetryIntervalMs(0L);
        return definition;
    }

    /** 判断主 JVM 中的任务调度定义是否符合预期。 */
    private static boolean scheduleMatches(
            JobHolder job, String scheduleType, String scheduleExpression) {
        if (job == null) {
            return false;
        }
        if ("FIXED_DELAY".equals(scheduleType)) {
            return Long.toString(job.getScheduled().fixedDelay())
                    .equals(scheduleExpression);
        }
        if ("FIXED_RATE".equals(scheduleType)) {
            return Long.toString(job.getScheduled().fixedRate())
                    .equals(scheduleExpression);
        }
        return job.getScheduled().cron().equals(scheduleExpression);
    }

    private void stopNodes() {
        nodes.forEach(ClusterNode::close);
        nodes.clear();
    }

    private static void await(String message, BooleanSupplier condition, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        RuntimeException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
                lastFailure = null;
            } catch (RuntimeException failure) {
                lastFailure = failure;
            }
            Thread.sleep(50L);
        }
        if (lastFailure != null) {
            throw new AssertionError(message, lastFailure);
        }
        throw new AssertionError(message);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(false);
            return socket.getLocalPort();
        }
    }

    private ClusterNode startNode(String name, long reconcileIntervalMs,
                                  Path classpathJar) throws Exception {
        for (int attempt = 1; attempt <= PORT_BIND_ATTEMPTS; attempt++) {
            try {
                return ClusterNode.start(name, freePort(), reconcileIntervalMs,
                        classpathJar, artifactDirectory);
            } catch (ClusterNodeStartupException failure) {
                if (!failure.addressInUse() || attempt == PORT_BIND_ATTEMPTS) {
                    throw failure;
                }
                Thread.sleep(50L * attempt);
            }
        }
        throw new IllegalStateException(name + " 子 JVM 启动重试状态异常");
    }

    private static Path createClasspathJar(Path output) throws Exception {
        LinkedHashSet<URI> classpath = new LinkedHashSet<>();
        addClasspath(classpath, System.getProperty("surefire.test.class.path"));
        addClasspath(classpath, System.getProperty("java.class.path"));
        for (ClassLoader loader = Thread.currentThread().getContextClassLoader();
             loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urlClassLoader) {
                for (URL url : urlClassLoader.getURLs()) {
                    if ("file".equalsIgnoreCase(url.getProtocol())) {
                        classpath.add(url.toURI());
                    }
                }
            }
        }
        for (Class<?> required : List.of(
                Application.class, ScheduledJobClusterIntegrationTest.class,
                ManagedSchedulingTestJob.class, Solon.class)) {
            classpath.add(required.getProtectionDomain().getCodeSource().getLocation().toURI());
        }
        assertFalse(classpath.isEmpty(), "无法构造子 JVM 测试类路径");

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH,
                classpath.stream().map(URI::toASCIIString).reduce((a, b) -> a + " " + b).orElseThrow());
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(output), manifest)) {
            return output;
        }
    }

    private static void addClasspath(Set<URI> classpath, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Arrays.stream(value.split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .filter(entry -> !entry.isBlank())
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(Files::exists)
                .map(Path::toUri)
                .forEach(classpath::add);
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    /**
     * 子节点任务状态。
     *
     * @param pid 子节点进程标识
     * @param registered 是否已注册
     * @param started 是否已启动
     * @param executions 测试处理器执行次数
     * @param scheduleType 调度类型
     * @param scheduleExpression 调度表达式
     * @param initialDelayMs 当前运行时首次执行延迟毫秒数
     */
    private record NodeState(long pid, boolean registered, boolean started,
                             int executions, String scheduleType,
                             String scheduleExpression, long initialDelayMs) {
    }

    private static final class ClusterNodeStartupException extends IllegalStateException {
        private final boolean addressInUse;

        private ClusterNodeStartupException(String message, boolean addressInUse) {
            super(message);
            this.addressInUse = addressInUse;
        }

        private boolean addressInUse() {
            return addressInUse;
        }
    }

    private static final class ClusterNode implements AutoCloseable {
        private static final HttpClient HTTP = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        private final String name;
        private final int port;
        private final Process process;
        private final Path outputLog;
        private final Path errorLog;

        private ClusterNode(String name, int port, Process process,
                            Path outputLog, Path errorLog) {
            this.name = name;
            this.port = port;
            this.process = process;
            this.outputLog = outputLog;
            this.errorLog = errorLog;
        }

        private static ClusterNode start(String name, int port, long reconcileIntervalMs,
                                         Path classpathJar, Path artifactDirectory) throws Exception {
            Path outputLog = artifactDirectory.resolve(name + "-" + port + ".out.log");
            Path errorLog = artifactDirectory.resolve(name + "-" + port + ".err.log");
            Path java = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").toLowerCase().contains("win")
                            ? "java.exe" : "java");
            ProcessBuilder builder = new ProcessBuilder(
                    java.toString(),
                    CHILD_JVM_INITIAL_HEAP,
                    CHILD_JVM_MAX_HEAP,
                    CHILD_JVM_GC,
                    "-Djimuqu.scheduling.reconcileIntervalMs="
                            + reconcileIntervalMs,
                    "-Djimuqu.scheduling.claimLeaseMs=" + CLAIM_LEASE_MS,
                    "-Dsecurity.excludes[2]=/__test/scheduler-cluster/**",
                    "-cp", classpathJar.toString(),
                    Application.class.getName(),
                    "--solon.env=test"
            );
            builder.directory(Path.of("").toAbsolutePath().toFile());
            builder.environment().put("JIMU_TEST_SERVER_PORT", Integer.toString(port));
            builder.environment().put("JIMU_TEST_SCHEDULER_CLUSTER_PROBE", "true");
            builder.environment().put("JIMU_TEST_SCHEDULING_RECONCILE_INTERVAL_MS",
                    Long.toString(reconcileIntervalMs));
            builder.environment().put("JIMU_TEST_OSS_DOMAIN",
                    "http://127.0.0.1:" + port + "/file/");
            builder.environment().put("JIMU_OSS_DOMAIN",
                    "http://127.0.0.1:" + port + "/file/");
            builder.redirectOutput(outputLog.toFile());
            builder.redirectError(errorLog.toFile());

            ClusterNode node = new ClusterNode(
                    name, port, builder.start(), outputLog, errorLog);
            try {
                node.awaitReady();
                return node;
            } catch (Throwable failure) {
                node.close();
                throw failure;
            }
        }

        private void awaitReady() throws InterruptedException {
            long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
            RuntimeException lastFailure = null;
            while (System.nanoTime() < deadline) {
                if (!process.isAlive()) {
                    String details = failureDetails();
                    throw new ClusterNodeStartupException(details, isAddressInUse(details));
                }
                try {
                    state();
                    return;
                } catch (RuntimeException failure) {
                    lastFailure = failure;
                }
                Thread.sleep(50L);
            }
            if (lastFailure != null) {
                throw new AssertionError(name + " 子 JVM 未就绪", lastFailure);
            }
            throw new AssertionError(name + " 子 JVM 未就绪");
        }

        /** 查询原有静态任务状态。 */
        private NodeState state() {
            return state(JOB_NAME);
        }

        /** 查询指定任务状态。 */
        private NodeState state(String jobName) {
            Map<String, Object> data = request(
                    "GET", "/state?jobName=" + jobName, true);
            return new NodeState(
                    ((Number) data.get("pid")).longValue(),
                    (Boolean) data.get("registered"),
                    (Boolean) data.get("started"),
                    ((Number) data.get("executions")).intValue(),
                    String.valueOf(data.get("scheduleType")),
                    String.valueOf(data.get("scheduleExpression")),
                    ((Number) data.get("initialDelayMs")).longValue()
            );
        }

        /** 触发指定任务。 */
        private void fire(String jobName) {
            request("POST", "/fire?jobName=" + jobName, false);
        }

        /** 切换当前子 JVM 的测试处理器模式。 */
        private void mode(String mode) {
            request("POST", "/mode?mode=" + mode, false);
        }

        /** 强制终止子 JVM，模拟认领后进程宕机。 */
        private void crash() throws InterruptedException {
            process.destroyForcibly();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS),
                    name + " 子 JVM 未在故障注入后退出");
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> request(String method, String path, boolean dataRequired) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(
                                "http://127.0.0.1:" + port + "/__test/scheduler-cluster" + path))
                        .timeout(HTTP_TIMEOUT)
                        .method(method, HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = HTTP.send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException(name + " HTTP " + response.statusCode()
                            + ": " + response.body());
                }
                Map<String, Object> envelope = JsonUtil.toObject(response.body(), Map.class);
                if (((Number) envelope.get("code")).intValue() != 200) {
                    throw new IllegalStateException(name + " 返回异常: " + response.body());
                }
                Object data = envelope.get("data");
                if (dataRequired && !(data instanceof Map<?, ?>)) {
                    throw new IllegalStateException(name + " 缺少对象数据: " + response.body());
                }
                return data instanceof Map<?, ?> ? (Map<String, Object>) data : Map.of();
            } catch (IOException failure) {
                throw new IllegalStateException(name + " HTTP 请求失败", failure);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(name + " HTTP 请求被中断", failure);
            }
        }

        private String failureDetails() {
            return name + " 子 JVM 已退出，exit=" + process.exitValue()
                    + "\nstdout:\n" + readLog(outputLog)
                    + "\nstderr:\n" + readLog(errorLog);
        }

        private static boolean isAddressInUse(String details) {
            String normalized = details.toLowerCase();
            return normalized.contains("address already in use")
                    || normalized.contains("java.net.bindexception")
                    || normalized.contains("only one usage of each socket address")
                    || details.contains("通常每个套接字地址");
        }

        private static String readLog(Path path) {
            try {
                String text = Files.exists(path)
                        ? new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                        : "";
                return text.length() <= 8_000 ? text : text.substring(text.length() - 8_000);
            } catch (IOException failure) {
                return "<读取日志失败: " + failure.getMessage() + ">";
            }
        }

        @Override
        public void close() {
            process.descendants().forEach(ProcessHandle::destroy);
            process.destroy();
            try {
                if (!process.waitFor(8, TimeUnit.SECONDS)) {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                    process.waitFor(8, TimeUnit.SECONDS);
                }
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    /**
     * 全局延迟调度器只能在当前 Surefire 测试计划结束后关闭，避免破坏同一 fork 的后续测试类。
     */
    private static final class TestPlanShutdown implements BeforeAllCallback,
            ExtensionContext.Store.CloseableResource {

        /** 测试计划根存储命名空间。 */
        private static final ExtensionContext.Namespace NAMESPACE =
                ExtensionContext.Namespace.create(TestPlanShutdown.class);

        /** 在测试计划根存储中注册一次关闭回调。 */
        @Override
        public void beforeAll(ExtensionContext context) {
            context.getRoot().getStore(NAMESPACE)
                    .getOrComputeIfAbsent(TestPlanShutdown.class, ignored -> this);
        }

        /** 清空远期延迟任务，并关闭 Solon 全局执行器。 */
        @Override
        public void close() {
            RunUtil.timer().shutdownNow();
            RunUtil.shutdown();
        }
    }

}
