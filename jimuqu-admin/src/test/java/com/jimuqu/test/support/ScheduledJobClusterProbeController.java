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

    /** 查询指定任务在当前测试节点的运行状态。 */
    @Get
    @Mapping("/state")
    public R<Map<String, Object>> state(String jobName) {
        JobHolder job = jobManager.jobGet(resolveJobName(jobName));
        return R.ok(Map.of(
                "pid", ProcessHandle.current().pid(),
                "registered", job != null,
                "started", job != null && isJobStarted(job),
                "executions", ManagedSchedulingTestJob.executions(),
                "scheduleType", job == null ? "" : scheduleType(job),
                "scheduleExpression", job == null ? "" : scheduleExpression(job),
                "initialDelayMs", job == null ? 0L
                        : job.getScheduled().initialDelay()
        ));
    }

    /** 在当前测试节点触发指定任务。 */
    @Post
    @Mapping("/fire")
    public R<Void> fire(String jobName) throws Throwable {
        requireJob(resolveJobName(jobName)).handle(new ContextEmpty());
        return R.ok();
    }

    /** 切换当前测试节点的处理器执行模式。 */
    @Post
    @Mapping("/mode")
    public R<Void> mode(String mode) {
        ManagedSchedulingTestJob.mode(
                ManagedSchedulingTestJob.Mode.valueOf(mode));
        return R.ok();
    }

    /** 判断原有静态集群测试任务是否已经启动。 */
    public static boolean isJobStarted(IJobManager jobManager) {
        JobHolder job = jobManager.jobGet(ManagedSchedulingTestJob.JOB_NAME);
        return job != null && isJobStarted(job);
    }

    /** 判断任务持有的简单调度器是否已经启动。 */
    private static boolean isJobStarted(JobHolder job) {
        return job.getAttachment() instanceof SimpleScheduler scheduler && scheduler.isStarted();
    }

    /** 解析空任务名为原有静态集群测试任务。 */
    private static String resolveJobName(String jobName) {
        return jobName == null || jobName.isBlank()
                ? ManagedSchedulingTestJob.JOB_NAME : jobName;
    }

    /** 获取必须存在的测试任务。 */
    private JobHolder requireJob(String jobName) {
        JobHolder job = jobManager.jobGet(jobName);
        if (job == null) {
            throw new IllegalStateException("集群测试任务未注册: " + jobName);
        }
        return job;
    }

    /** 获取测试任务的调度类型。 */
    private static String scheduleType(JobHolder job) {
        if (job.getScheduled().fixedDelay() > 0) {
            return "FIXED_DELAY";
        }
        if (job.getScheduled().fixedRate() > 0) {
            return "FIXED_RATE";
        }
        return "CRON";
    }

    /** 获取测试任务的调度表达式。 */
    private static String scheduleExpression(JobHolder job) {
        if (job.getScheduled().fixedDelay() > 0) {
            return Long.toString(job.getScheduled().fixedDelay());
        }
        if (job.getScheduled().fixedRate() > 0) {
            return Long.toString(job.getScheduled().fixedRate());
        }
        return job.getScheduled().cron();
    }
}
