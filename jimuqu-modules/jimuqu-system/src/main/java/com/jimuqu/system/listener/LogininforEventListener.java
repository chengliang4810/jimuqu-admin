package com.jimuqu.system.listener;

import com.jimuqu.common.log.event.LogininforEvent;
import com.jimuqu.system.service.SysLoginInfoService;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.event.EventListener;

import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class LogininforEventListener implements EventListener<LogininforEvent> {

    private final SysLoginInfoService service;
    private final ExecutorService executorService;

    public LogininforEventListener(SysLoginInfoService service,
                                   ExecutorService executorService) {
        this.service = service;
        this.executorService = executorService;
    }

    @Override
    public void onEvent(LogininforEvent event) {
        try {
            executorService.execute(() -> {
                try {
                    service.record(event);
                } catch (RuntimeException exception) {
                    log.error("记录登录日志异常", exception);
                }
            });
        } catch (RuntimeException exception) {
            log.error("提交登录日志任务异常", exception);
        }
    }
}
