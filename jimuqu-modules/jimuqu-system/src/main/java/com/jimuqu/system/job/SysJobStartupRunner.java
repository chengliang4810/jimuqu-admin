package com.jimuqu.system.job;

import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.bean.LifecycleBean;

/**
 * 系统启动后恢复启用中的在线定时任务。
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Component
@RequiredArgsConstructor
public class SysJobStartupRunner implements LifecycleBean {

    private final SysJobHandlerRegistry handlerRegistry;
    private final SysJobScheduler sysJobScheduler;

    @Override
    public void start() {
        handlerRegistry.refresh();
        sysJobScheduler.restoreEnabledJobs();
    }
}
