package com.jimuqu.common.sse.config;

import lombok.Data;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * SSE 配置项
 */
@Data
@Inject("${sse}")
@Configuration
public class SseProperties {

    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 路径
     */
    private String path;
    /**
     * 心跳开启
     */
    private Boolean heartbeat;
    /**
     * 心跳间隔
     */
    private Long heartbeatInterval;

}
