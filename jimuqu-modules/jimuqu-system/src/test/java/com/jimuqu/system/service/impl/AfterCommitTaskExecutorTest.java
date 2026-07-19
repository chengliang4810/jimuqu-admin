package com.jimuqu.system.service.impl;

import org.junit.jupiter.api.Test;
import org.noear.solon.data.tran.TranListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AfterCommitTaskExecutorTest {

    @Test
    void rollbackCompletionDoesNotSubmitTask() {
        ExecutorService executor = mock(ExecutorService.class);
        AfterCommitTaskExecutor taskExecutor = new AfterCommitTaskExecutor(executor);
        TranListener listener = taskExecutor.afterCommitListener(() -> { });

        listener.afterCompletion(TranListener.STATUS_ROLLED_BACK);

        verify(executor, never()).execute(any());
    }

    @Test
    void submitsOnlyAfterCommitCallback() {
        ExecutorService executor = mock(ExecutorService.class);
        AtomicInteger executions = new AtomicInteger();
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(executor).execute(any());
        AfterCommitTaskExecutor taskExecutor = new AfterCommitTaskExecutor(executor);
        TranListener listener = taskExecutor.afterCommitListener(executions::incrementAndGet);

        assertEquals(0, executions.get());
        listener.afterCommit();

        assertEquals(1, executions.get());
    }

    @Test
    void taskFailureDoesNotEscapeCommitCallback() {
        ExecutorService executor = mock(ExecutorService.class);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(executor).execute(any());
        AfterCommitTaskExecutor taskExecutor = new AfterCommitTaskExecutor(executor);
        TranListener listener = taskExecutor.afterCommitListener(() -> {
            throw new IllegalStateException("cleanup failed");
        });

        assertDoesNotThrow(listener::afterCommit);
    }

    @Test
    void rejectedSubmissionDoesNotEscapeCommitCallback() {
        ExecutorService executor = mock(ExecutorService.class);
        doThrow(new RejectedExecutionException("shutdown")).when(executor).execute(any());
        AfterCommitTaskExecutor taskExecutor = new AfterCommitTaskExecutor(executor);
        TranListener listener = taskExecutor.afterCommitListener(() -> { });

        assertDoesNotThrow(listener::afterCommit);
    }
}
