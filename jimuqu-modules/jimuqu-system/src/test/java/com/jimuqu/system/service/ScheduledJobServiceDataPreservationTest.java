package com.jimuqu.system.service;

import com.jimuqu.system.domain.SysScheduledJobConfig;
import com.jimuqu.system.mapper.SysScheduledJobLogMapper;
import org.junit.jupiter.api.Test;
import org.noear.solon.scheduling.annotation.Scheduled;
import org.noear.solon.scheduling.scheduled.JobHolder;
import org.noear.solon.scheduling.scheduled.manager.IJobManager;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledJobServiceDataPreservationTest {

    @Test
    void preservesRegisteredDataAcrossInitialAndRepeatedReconciliation() throws Exception {
        IJobManager jobManager = mock(IJobManager.class);
        ScheduledJobConfigService configService = mock(ScheduledJobConfigService.class);
        JobHolder job = mock(JobHolder.class);
        Scheduled scheduled = mock(Scheduled.class);
        Map<String, String> registeredData = Map.of("tenant", "jimuqu");
        SysScheduledJobConfig config = new SysScheduledJobConfig().setEnabled(true);

        when(job.getName()).thenReturn("dataAwareJob");
        when(job.getScheduled()).thenReturn(scheduled);
        when(job.getData()).thenReturn(registeredData);
        when(scheduled.enable()).thenReturn(true);
        when(jobManager.jobGetAll()).thenReturn(Map.of("dataAwareJob", job));
        when(configService.getOrCreate("dataAwareJob", true)).thenReturn(config);

        ScheduledJobService service = new ScheduledJobService(
                jobManager,
                mock(ExecutorService.class),
                mock(ScheduledExecutorService.class),
                configService,
                mock(SysScheduledJobLogMapper.class)
        );

        Method reconcile = ScheduledJobService.class.getDeclaredMethod("reconcileLocalJobs");
        reconcile.setAccessible(true);
        reconcile.invoke(service);
        reconcile.invoke(service);

        verify(jobManager, times(2))
                .jobStart(eq("dataAwareJob"), same(registeredData));
    }

    @Test
    void serializesConcurrentStartAndStopStateApplications() throws Exception {
        IJobManager jobManager = mock(IJobManager.class);
        JobHolder job = mock(JobHolder.class);
        when(job.getName()).thenReturn("serializedJob");
        when(job.getData()).thenReturn(Map.of());
        ScheduledJobService service = new ScheduledJobService(
                jobManager,
                mock(ExecutorService.class),
                mock(ScheduledExecutorService.class),
                mock(ScheduledJobConfigService.class),
                mock(SysScheduledJobLogMapper.class)
        );
        Method apply = ScheduledJobService.class.getDeclaredMethod(
                "applyLocalState", JobHolder.class, SysScheduledJobConfig.class);
        apply.setAccessible(true);
        CountDownLatch startEntered = new CountDownLatch(1);
        CountDownLatch releaseStart = new CountDownLatch(1);
        List<String> events = new CopyOnWriteArrayList<>();
        doAnswer(ignored -> {
            events.add("start");
            startEntered.countDown();
            assertTrue(releaseStart.await(2, TimeUnit.SECONDS));
            events.add("started");
            return null;
        }).when(jobManager).jobStart("serializedJob", Map.of());
        doAnswer(ignored -> {
            events.add("stop");
            return null;
        }).when(jobManager).jobStop("serializedJob");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> starting = pool.submit(() ->
                apply.invoke(service, job, new SysScheduledJobConfig().setEnabled(true)));
        try {
            assertTrue(startEntered.await(2, TimeUnit.SECONDS));
            Future<?> stopping = pool.submit(() ->
                    apply.invoke(service, job, new SysScheduledJobConfig().setEnabled(false)));
            Thread.sleep(100L);
            assertFalse(stopping.isDone(), "stop must wait for the in-flight local start");

            releaseStart.countDown();
            starting.get(2, TimeUnit.SECONDS);
            stopping.get(2, TimeUnit.SECONDS);
            assertEquals(List.of("start", "started", "stop"), events);
        } finally {
            releaseStart.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
        }
    }
}
