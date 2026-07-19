package com.jimuqu.system.service.impl;

import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranPolicy;
import org.noear.solon.data.tran.TranUtils;

import java.util.Objects;

/**
 * 以独立事务执行单行用户导入，避免聚合错误回滚已经成功的其他行。
 */
@Component
public class SysUserImportRowTransactionExecutor {

    private static final Transaction ROW_TRANSACTION = new TransactionAnno()
            .policy(TranPolicy.requires_new);

    public void execute(Runnable task) {
        Objects.requireNonNull(task, "用户导入任务不能为空");
        try {
            TranUtils.execute(ROW_TRANSACTION, task::run);
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new IllegalStateException("执行用户导入事务失败", throwable);
        }
    }

    static TranPolicy transactionPolicy() {
        return ROW_TRANSACTION.policy();
    }
}
