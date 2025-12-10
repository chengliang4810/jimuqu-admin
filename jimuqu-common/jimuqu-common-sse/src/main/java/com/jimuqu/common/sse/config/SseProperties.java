package com.jimuqu.common.sse.config;

import lombok.Data;

/**
 * SSE 配置项
 */
@Data
public class SseProperties {

    private Boolean enabled;

    /**
     * 路径
     */
    private String path;
}
