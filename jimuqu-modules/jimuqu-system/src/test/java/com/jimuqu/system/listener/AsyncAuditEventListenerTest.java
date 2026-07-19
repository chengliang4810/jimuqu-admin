package com.jimuqu.system.listener;

import com.jimuqu.common.log.event.LogininforEvent;
import com.jimuqu.common.log.event.OperLogEvent;
import com.jimuqu.system.service.SysClientService;
import com.jimuqu.system.service.SysLoginInfoService;
import com.jimuqu.system.service.SysOperLogService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class AsyncAuditEventListenerTest {

    @Test
    void loginAuditFailureRunsOffRequestThreadAndDoesNotEscape() throws Exception {
        AtomicReference<Thread> worker = new AtomicReference<>();
        SysLoginInfoService failingService = new SysLoginInfoService(null, (SysClientService) null) {
            @Override
            public void record(LogininforEvent event) {
                worker.set(Thread.currentThread());
                throw new IllegalStateException("login audit unavailable");
            }
        };

        verifyAsyncFailureIsolated(executor ->
                new LogininforEventListener(failingService, executor).onEvent(new LogininforEvent()), worker);
    }

    @Test
    void operationAuditFailureRunsOffRequestThreadAndDoesNotEscape() throws Exception {
        AtomicReference<Thread> worker = new AtomicReference<>();
        SysOperLogService failingService = new SysOperLogService(null) {
            @Override
            public void record(OperLogEvent event) {
                worker.set(Thread.currentThread());
                throw new IllegalStateException("operation audit unavailable");
            }
        };

        verifyAsyncFailureIsolated(executor ->
                new OperLogEventListener(failingService, executor).onEvent(new OperLogEvent()), worker);
    }

    private void verifyAsyncFailureIsolated(ListenerCall call, AtomicReference<Thread> worker) throws Exception {
        Thread requestThread = Thread.currentThread();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertDoesNotThrow(() -> call.accept(executor));
            executor.submit(() -> { }).get(5, TimeUnit.SECONDS);
            assertNotSame(requestThread, worker.get());
        } finally {
            executor.shutdownNow();
        }

        assertDoesNotThrow(() -> call.accept(executor));
    }

    @FunctionalInterface
    private interface ListenerCall {
        void accept(ExecutorService executor);
    }
}
