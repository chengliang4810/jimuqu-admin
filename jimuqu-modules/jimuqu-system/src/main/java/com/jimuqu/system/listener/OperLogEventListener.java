package com.jimuqu.system.listener;

import com.jimuqu.common.log.event.OperLogEvent;
import com.jimuqu.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.event.EventListener;

@Component
@RequiredArgsConstructor
public class OperLogEventListener implements EventListener<OperLogEvent> {

    private final SysOperLogService service;

    @Override
    public void onEvent(OperLogEvent event) {
        service.record(event);
    }
}
