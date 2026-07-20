package com.jimuqu.system.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.tran.TranListener;
import org.noear.solon.data.tran.TranUtils;

import java.util.Objects;

/**
 * 在事务提交后同步执行依赖已提交数据的副作用。
 */
@Slf4j
@Component
public class AfterCommitTaskExecutor {

    public void execute(Runnable task) {
        Objects.requireNonNull(task, "提交后任务不能为空");
        if (!TranUtils.inTrans()) {
            runSafely(task);
            return;
        }
        TranUtils.listen(afterCommitListener(task));
    }

    TranListener afterCommitListener(Runnable task) {
        return new TranListener() {
            @Override
            public void afterCommit() {
                runSafely(task);
            }
        };
    }

    private void runSafely(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException ex) {
            log.error("执行事务提交后任务失败", ex);
        }
    }
}
