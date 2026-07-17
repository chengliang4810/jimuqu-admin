package com.jimuqu.system.listener;

import com.jimuqu.common.log.event.LogininforEvent;
import com.jimuqu.system.service.SysLoginInfoService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.event.EventListener;

@Component
@RequiredArgsConstructor
public class LogininforEventListener implements EventListener<LogininforEvent> {

    private final SysLoginInfoService service;

    @Override
    public void onEvent(LogininforEvent event) {
        service.record(event);
    }
}
