package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.Application;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.redis.utils.RedisUtils;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import com.jimuqu.test.support.ManagedSchedulingTestJob;
import com.jimuqu.test.support.ScheduledJobClusterProbeController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.scheduling.scheduled.JobHolder;
import org.noear.solon.scheduling.scheduled.manager.IJobManager;
import org.noear.solon.test.SolonTest;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 两个额外 Application JVM 与测试 JVM 共享 MySQL/Redis 时的调度集群契约。
 */
@SolonTest(value = Application.class, env = "test", debug = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ScheduledJobClusterIntegrationTest {

    private static final String JOB_NAME = ManagedSchedulingTestJob.JOB_NAME;
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration STATE_TIMEOUT = Duration.ofSeconds(8);
    private static final int PORT_BIND_ATTEMPTS = 5;

    @Inject
    private ScheduledJobService jobService;
    @Inject
    private ScheduledJobConfigService configService;
    @Inject
    private SysScheduledJobLogMapper logMapper;
    @Inject
    private IJobManager jobManager;

    private final List<ClusterNode> nodes = new ArrayList<>();
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
            nodes.add(startNode("fast-reconcile", 100L, classpathJar));
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

    @Test
    void coordinatesControlReconciliationAndMutualExclusionAcrossThreeJvms() throws Exception {
        ClusterNode slowReconcile = nodes.get(0);
        ClusterNode fastReconcile = nodes.get(1);

        jobService.start(JOB_NAME);
        await("Pub/Sub 必须启动全部 JVM 的任务",
                () -> mainStarted() && slowReconcile.state().started()
                        && fastReconcile.state().started(), STATE_TIMEOUT);

        jobService.stop(JOB_NAME);
        await("Pub/Sub 必须停止全部 JVM 的任务",
                () -> !mainStarted() && !slowReconcile.state().started()
                        && !fastReconcile.state().started(), STATE_TIMEOUT);

        jobService.start(JOB_NAME);
        await("对账测试前全部任务必须启动",
                () -> mainStarted() && slowReconcile.state().started()
                        && fastReconcile.state().started(), STATE_TIMEOUT);
        configService.updateEnabled(JOB_NAME, false, false);
        await("未发布消息时快速节点和测试 JVM 必须依靠周期对账停止",
                () -> !mainStarted() && !fastReconcile.state().started(), STATE_TIMEOUT);
        assertTrue(slowReconcile.state().started(),
                "十分钟对账节点不得在测试窗口内自行停止，避免把周期对账误判为 Pub/Sub");

        jobService.start(JOB_NAME);
        await("集群互斥测试前全部任务必须恢复",
                () -> mainStarted() && slowReconcile.state().started()
                        && fastReconcile.state().started(), STATE_TIMEOUT);
        verifySingleClusterExecution(slowReconcile, fastReconcile);
    }

    private void verifySingleClusterExecution(
            ClusterNode firstNode, ClusterNode secondNode) throws Exception {
        clearExecutionEvidence();
        ManagedSchedulingTestJob.mode(ManagedSchedulingTestJob.Mode.SUCCESS);
        int before = ManagedSchedulingTestJob.executions()
                + firstNode.state().executions()
                + secondNode.state().executions();

        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Callable<Void>> calls = List.of(
                    () -> runTogether(ready, start, this::fireMain),
                    () -> runTogether(ready, start, firstNode::fire),
                    () -> runTogether(ready, start, secondNode::fire)
            );
            List<Future<Void>> results = calls.stream().map(executor::submit).toList();
            assertTrue(ready.await(5, TimeUnit.SECONDS), "三个 JVM 未同时准备好任务执行");
            start.countDown();
            for (Future<Void> result : results) {
                result.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "集群测试线程池未退出");
        }

        int after = ManagedSchedulingTestJob.executions()
                + firstNode.state().executions()
                + secondNode.state().executions();
        assertEquals(1, after - before, "同一调度周期三个 JVM 只能有一个执行原始任务");

        List<SysScheduledJobLog> logs = QueryChain.of(logMapper)
                .eq(SysScheduledJobLog::getJobName, JOB_NAME)
                .eq(SysScheduledJobLog::getTriggerType, "SCHEDULED")
                .list();
        assertEquals(3, logs.size(), "三个 JVM 的一次竞争都必须留下执行日志");
        assertEquals(1L, logs.stream().filter(log -> "SUCCESS".equals(log.getStatus())).count());
        assertEquals(2L, logs.stream().filter(log -> "SKIPPED".equals(log.getStatus())).count());
        assertEquals(3L, logs.stream().map(SysScheduledJobLog::getInstanceId).distinct().count(),
                "日志必须证明三个独立 JVM 都参与了同一次竞争");
    }

    private Void runTogether(CountDownLatch ready, CountDownLatch start,
                             CheckedRunnable action) throws Exception {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS), "等待集群同步执行超时");
        action.run();
        return null;
    }

    private void fireMain() throws Exception {
        JobHolder job = jobManager.jobGet(JOB_NAME);
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
        if (logMapper != null) {
            logMapper.delete(where -> where.eq(SysScheduledJobLog::getJobName, JOB_NAME));
        }
        if (Solon.app() != null) {
            String prefix = Solon.cfg().get("jimuqu.cache.keyHeader", "jimuqu");
            String separator = prefix.endsWith(":") ? "" : ":";
            RedisUtils.getClient().getBucket(prefix + separator + "scheduled-job:{"
                    + JOB_NAME + "}:scheduled:marker").delete();
        }
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

    private record NodeState(long pid, boolean started, int executions) {
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

        private NodeState state() {
            Map<String, Object> data = request("GET", "/state", true);
            return new NodeState(
                    ((Number) data.get("pid")).longValue(),
                    (Boolean) data.get("started"),
                    ((Number) data.get("executions")).intValue()
            );
        }

        private void fire() {
            request("POST", "/fire", false);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> request(String method, String path, boolean dataRequired) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(
                                "http://127.0.0.1:" + port + "/__test/scheduler-cluster" + path))
                        .timeout(Duration.ofSeconds(5))
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
}
