package com.jimuqu.system.service;

import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.domain.bo.ScheduledJobConfigBo;
import com.jimuqu.system.domain.bo.ScheduledJobDefinitionBo;
import com.jimuqu.system.mapper.SysScheduledJobConfigMapper;
import db.sql.api.impl.cmd.struct.Where;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * 定时任务配置删除重建期间的配置代际隔离测试。
 */
class ScheduledJobConfigServiceGenerationTest {

    /** 验证动态定义更新不得跨越删除重建后的新配置代际。 */
    @Test
    void updateDynamicDoesNotWriteTheRecreatedGeneration() {
        SysScheduledJobConfigMapper mapper = mock(SysScheduledJobConfigMapper.class);
        AtomicReference<SysScheduledJobConfig> current =
                new AtomicReference<>(dynamicConfig(11L, 101L));
        ControlledConfigService service =
                new ControlledConfigService(mapper, current);
        List<Long> updatedConfigIds = captureFailedFirstUpdate(
                mapper, current, dynamicConfig(22L, 201L));

        ServiceException failure = assertThrows(ServiceException.class,
                () -> service.updateDynamic(
                        "generationJob", dynamicDefinition("generationJob")));

        assertTrue(failure.getMessage().contains("删除并重建"));
        assertEquals(List.of(11L), updatedConfigIds,
                "旧更新请求不得把新定义写入同名重建后的 configId");
        assertEquals(0, service.systemCreateAttempts(),
                "动态定义更新不得创建 SYSTEM 配置");
    }

    /** 验证启停更新不得跨越删除重建后的新配置代际。 */
    @Test
    void updateEnabledDoesNotWriteTheRecreatedGeneration() {
        SysScheduledJobConfigMapper mapper = mock(SysScheduledJobConfigMapper.class);
        AtomicReference<SysScheduledJobConfig> current =
                new AtomicReference<>(dynamicConfig(31L, 301L));
        ControlledConfigService service =
                new ControlledConfigService(mapper, current);
        List<Long> updatedConfigIds = captureFailedFirstUpdate(
                mapper, current, dynamicConfig(42L, 401L));

        ServiceException failure = assertThrows(ServiceException.class,
                () -> service.updateEnabled("generationJob", false, true));

        assertTrue(failure.getMessage().contains("删除并重建"));
        assertEquals(List.of(31L), updatedConfigIds,
                "旧启停请求不得写入同名重建后的 configId");
        assertEquals(0, service.systemCreateAttempts(),
                "旧启停请求不得创建 SYSTEM 配置");
    }

    /** 验证删除空窗期间的旧启停请求不会补建 SYSTEM 配置。 */
    @Test
    void updateEnabledDoesNotCreateSystemConfigDuringDeleteGap() {
        SysScheduledJobConfigMapper mapper = mock(SysScheduledJobConfigMapper.class);
        AtomicReference<SysScheduledJobConfig> current =
                new AtomicReference<>(dynamicConfig(51L, 501L));
        ControlledConfigService service =
                new ControlledConfigService(mapper, current);
        List<Long> updatedConfigIds = captureFailedFirstUpdate(
                mapper, current, null);

        ServiceException failure = assertThrows(ServiceException.class,
                () -> service.updateEnabled("generationJob", false, true));

        assertTrue(failure.getMessage().contains("已被删除"));
        assertEquals(List.of(51L), updatedConfigIds);
        assertEquals(0, service.systemCreateAttempts(),
                "检测到旧配置被删除后不得调用 getOrCreate 创建 SYSTEM 行");
    }

    /** 验证启停更新保留调度定义时间。 */
    @Test
    void updateEnabledPreservesDefinitionTime() {
        SysScheduledJobConfigMapper mapper =
                mock(SysScheduledJobConfigMapper.class);
        Date definitionTime = new Date(1_750_000_000_000L);
        SysScheduledJobConfig config = dynamicConfig(61L, 601L);
        config.setUpdateTime(definitionTime);
        AtomicReference<SysScheduledJobConfig> current =
                new AtomicReference<>(config);
        ControlledConfigService service =
                new ControlledConfigService(mapper, current);
        AtomicReference<SysScheduledJobConfig> update =
                captureSuccessfulUpdate(mapper);

        service.updateEnabled("generationJob", false, true);

        assertEquals(definitionTime, update.get().getUpdateTime());
    }

    /** 验证重试配置更新保留调度定义时间。 */
    @Test
    void updateRetryPreservesDefinitionTime() {
        SysScheduledJobConfigMapper mapper =
                mock(SysScheduledJobConfigMapper.class);
        Date definitionTime = new Date(1_750_000_000_000L);
        SysScheduledJobConfig config = dynamicConfig(62L, 602L);
        config.setUpdateTime(definitionTime);
        AtomicReference<SysScheduledJobConfig> current =
                new AtomicReference<>(config);
        ControlledConfigService service =
                new ControlledConfigService(mapper, current);
        AtomicReference<SysScheduledJobConfig> update =
                captureSuccessfulUpdate(mapper);
        ScheduledJobConfigBo retry = new ScheduledJobConfigBo();
        retry.setMaxRetries(2);
        retry.setRetryIntervalMs(500L);

        service.updateRetry("generationJob", true, retry);

        assertEquals(definitionTime, update.get().getUpdateTime());
        assertEquals(
                ScheduledJobConfigService.CONCURRENT_FORBID,
                update.get().getConcurrentPolicy(),
                "启用失败重试必须同时禁止同一任务并发执行");
    }

    /** 验证编辑动态定义不会绕过独立启停操作修改状态。 */
    @Test
    void updateDynamicPreservesEnabledState() {
        SysScheduledJobConfigMapper mapper =
                mock(SysScheduledJobConfigMapper.class);
        SysScheduledJobConfig config = dynamicConfig(63L, 603L)
                .setEnabled(true);
        AtomicReference<SysScheduledJobConfig> current =
                new AtomicReference<>(config);
        ControlledConfigService service =
                new ControlledConfigService(mapper, current);
        AtomicReference<SysScheduledJobConfig> update =
                captureSuccessfulUpdate(mapper);

        service.updateDynamic(
                "generationJob", dynamicDefinition("generationJob"));

        assertTrue(update.get().getEnabled(),
                "编辑定义必须保留原启停状态");
    }

    /** 捕获首轮乐观锁失败，并把持久化状态切换到指定代际。 */
    private static List<Long> captureFailedFirstUpdate(
            SysScheduledJobConfigMapper mapper,
            AtomicReference<SysScheduledJobConfig> current,
            SysScheduledJobConfig next) {
        List<Long> updatedConfigIds = new ArrayList<>();
        doAnswer(invocation -> {
            SysScheduledJobConfig update = invocation.getArgument(0);
            updatedConfigIds.add(update.getConfigId());
            current.set(next);
            return updatedConfigIds.size() == 1 ? 0 : 1;
        }).when(mapper).update(
                any(SysScheduledJobConfig.class),
                ArgumentMatchers.<Consumer<Where>>any());
        return updatedConfigIds;
    }

    /** 捕获一次成功的配置更新。 */
    private static AtomicReference<SysScheduledJobConfig> captureSuccessfulUpdate(
            SysScheduledJobConfigMapper mapper) {
        AtomicReference<SysScheduledJobConfig> update = new AtomicReference<>();
        doAnswer(invocation -> {
            update.set(invocation.getArgument(0));
            return 1;
        }).when(mapper).update(
                any(SysScheduledJobConfig.class),
                ArgumentMatchers.<Consumer<Where>>any());
        return update;
    }

    /** 构造测试使用的动态配置代际。 */
    private static SysScheduledJobConfig dynamicConfig(
            long configId, long version) {
        return new SysScheduledJobConfig()
                .setConfigId(configId)
                .setJobName("generationJob")
                .setJobSource(ScheduledJobConfigService.SOURCE_DYNAMIC)
                .setEnabled(true)
                .setControlVersion(version);
    }

    /** 构造动态定义更新请求。 */
    private static ScheduledJobDefinitionBo dynamicDefinition(String jobName) {
        ScheduledJobDefinitionBo definition = new ScheduledJobDefinitionBo();
        definition.setJobName(jobName);
        definition.setDescription("配置代际测试任务");
        definition.setHandlerKey("test.generation");
        definition.setScheduleType("FIXED_RATE");
        definition.setScheduleExpression("1000");
        definition.setZone("");
        definition.setInitialDelayMs(0L);
        definition.setConcurrentPolicy("FORBID");
        definition.setMisfirePolicy("IGNORE");
        definition.setMaxRetries(0);
        definition.setRetryIntervalMs(0L);
        return definition;
    }

    /**
     * 通过可控持久化快照模拟删除和同名重建。
     */
    private static final class ControlledConfigService
            extends ScheduledJobConfigService {

        /** 当前可见的持久化配置。 */
        private final AtomicReference<SysScheduledJobConfig> current;

        /** 非法补建 SYSTEM 配置的尝试次数。 */
        private final AtomicInteger systemCreateAttempts = new AtomicInteger();

        /** 创建可控配置服务。 */
        private ControlledConfigService(
                SysScheduledJobConfigMapper mapper,
                AtomicReference<SysScheduledJobConfig> current) {
            super(mapper);
            this.current = current;
        }

        /** 返回测试控制的当前配置快照。 */
        @Override
        public SysScheduledJobConfig find(String jobName) {
            return current.get();
        }

        /** 记录任何不应发生的 SYSTEM 配置补建。 */
        @Override
        public SysScheduledJobConfig getOrCreate(
                String jobName, boolean defaultEnabled) {
            systemCreateAttempts.incrementAndGet();
            SysScheduledJobConfig created = new SysScheduledJobConfig()
                    .setConfigId(99L)
                    .setJobName(jobName)
                    .setJobSource(SOURCE_SYSTEM)
                    .setEnabled(defaultEnabled)
                    .setControlVersion(0L);
            current.set(created);
            return created;
        }

        /** 获取非法补建 SYSTEM 配置的尝试次数。 */
        private int systemCreateAttempts() {
            return systemCreateAttempts.get();
        }
    }
}
