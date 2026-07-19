package com.jimuqu.system.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.tran.TranListener;
import org.noear.solon.data.tran.TranUtils;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 在事务提交后异步执行不应影响业务写入的副作用。
 */
@Slf4j
@Component
public class AfterCommitTaskExecutor {

    private final ExecutorService executorService;

    public AfterCommitTaskExecutor(ExecutorService executorService) {
        this.executorService = Objects.requireNonNull(executorService, "executorService");
    }

    public void execute(Runnable task) {
        Objects.requireNonNull(task, "提交后任务不能为空");
        if (!TranUtils.inTrans()) {
            submit(task);
            return;
        }
        try {
            TranUtils.listen(afterCommitListener(task));
        } catch (RuntimeException ex) {
            log.error("注册事务提交后任务失败", ex);
        }
    }

    TranListener afterCommitListener(Runnable task) {
        return new TranListener() {
            @Override
            public void afterCommit() {
                submit(task);
            }
        };
    }

    private void submit(Runnable task) {
        try {
            executorService.execute(() -> {
                try {
                    task.run();
                } catch (RuntimeException ex) {
                    log.error("执行事务提交后任务失败", ex);
                }
            });
        } catch (RuntimeException ex) {
            log.error("提交事务后异步任务失败", ex);
        }
    }
}
