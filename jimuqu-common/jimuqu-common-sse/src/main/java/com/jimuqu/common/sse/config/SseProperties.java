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

    private Boolean enabled;

    /**
     * 路径
     */
    private String path;

}
