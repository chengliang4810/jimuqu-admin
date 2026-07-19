package com.jimuqu.system.listener;

import com.jimuqu.common.log.event.OperLogEvent;
import com.jimuqu.system.service.SysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.event.EventListener;

import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class OperLogEventListener implements EventListener<OperLogEvent> {

    private final SysOperLogService service;
    private final ExecutorService executorService;

    public OperLogEventListener(SysOperLogService service,
                                ExecutorService executorService) {
        this.service = service;
        this.executorService = executorService;
    }

    @Override
    public void onEvent(OperLogEvent event) {
        try {
            executorService.execute(() -> {
                try {
                    service.record(event);
                } catch (RuntimeException exception) {
                    log.error("记录操作日志异常", exception);
                }
            });
        } catch (RuntimeException exception) {
            log.error("提交操作日志任务异常", exception);
        }
    }
}
