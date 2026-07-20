package com.jimuqu.system.service.impl;

import com.jimuqu.common.sms.config.SmsConfig;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranPolicy;
import org.noear.solon.data.tran.TranUtils;
import org.noear.solon.test.SolonTest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(SmsConfig.class)
@SolonTest(scanning = false, enableHttp = false, debug = false, delay = 0)
public class AfterCommitTaskExecutorTest {

    @Test
    void rollbackDoesNotRunTask() {
        AfterCommitTaskExecutor taskExecutor = new AfterCommitTaskExecutor();
        AtomicInteger executions = new AtomicInteger();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> inTransaction(() -> {
            taskExecutor.execute(executions::incrementAndGet);
            throw new IllegalStateException("rollback");
        }));

        assertEquals("rollback", exception.getMessage());
        assertEquals(0, executions.get());
    }

    @Test
    void committedTaskCompletesSynchronouslyBeforeTransactionReturns() throws Throwable {
        AfterCommitTaskExecutor taskExecutor = new AfterCommitTaskExecutor();
        AtomicInteger executions = new AtomicInteger();
        AtomicLong taskThread = new AtomicLong();
        long transactionThread = Thread.currentThread().getId();

        inTransaction(() -> {
            taskExecutor.execute(() -> {
                taskThread.set(Thread.currentThread().getId());
                executions.incrementAndGet();
            });
            assertEquals(0, executions.get());
        });

        assertEquals(1, executions.get());
        assertEquals(transactionThread, taskThread.get());
    }

    @Test
    void taskFailureDoesNotEscapeTransactionCommit() {
        AfterCommitTaskExecutor taskExecutor = new AfterCommitTaskExecutor();

        assertDoesNotThrow(() -> inTransaction(() -> taskExecutor.execute(() -> {
            throw new IllegalStateException("cleanup failed");
        })));
    }

    @Test
    void executesImmediatelyWithoutTransaction() {
        AfterCommitTaskExecutor taskExecutor = new AfterCommitTaskExecutor();
        AtomicInteger executions = new AtomicInteger();

        taskExecutor.execute(executions::incrementAndGet);

        assertEquals(1, executions.get());
    }

    private static void inTransaction(Runnable task) throws Throwable {
        TranUtils.execute(new TransactionAnno().policy(TranPolicy.required), task::run);
    }
}
