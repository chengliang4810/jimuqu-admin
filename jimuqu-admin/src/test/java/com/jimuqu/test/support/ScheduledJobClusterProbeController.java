package com.jimuqu.test.support;

import cn.dev33.satoken.annotation.SaIgnore;
import com.jimuqu.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.scheduling.scheduled.JobHolder;
import org.noear.solon.scheduling.scheduled.manager.IJobManager;
import org.noear.solon.scheduling.simple.SimpleScheduler;

import java.util.Map;

/**
 * 仅供真实多进程测试观察子 JVM 中的 Solon 调度器。
 */
@Controller
@SaIgnore
@RequiredArgsConstructor
@Mapping("/__test/scheduler-cluster")
@Condition(onExpression = "${jimuqu.test.schedulerClusterProbe:false} == true")
public class ScheduledJobClusterProbeController {

    private final IJobManager jobManager;

    @Get
    @Mapping("/state")
    public R<Map<String, Object>> state() {
        JobHolder job = requireJob();
        return R.ok(Map.of(
                "pid", ProcessHandle.current().pid(),
                "started", isJobStarted(job),
                "executions", ManagedSchedulingTestJob.executions()
        ));
    }

    @Post
    @Mapping("/fire")
    public R<Void> fire() throws Throwable {
        requireJob().handle(new ContextEmpty());
        return R.ok();
    }

    public static boolean isJobStarted(IJobManager jobManager) {
        JobHolder job = jobManager.jobGet(ManagedSchedulingTestJob.JOB_NAME);
        return job != null && isJobStarted(job);
    }

    private static boolean isJobStarted(JobHolder job) {
        return job.getAttachment() instanceof SimpleScheduler scheduler && scheduler.isStarted();
    }

    private JobHolder requireJob() {
        JobHolder job = jobManager.jobGet(ManagedSchedulingTestJob.JOB_NAME);
        if (job == null) {
            throw new IllegalStateException("集群测试任务未注册");
        }
        return job;
    }
}
