package com.jimuqu.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 在线定时任务可调用处理器。
 */
@Data
@AllArgsConstructor
public class ScheduledJobHandlerVo implements Serializable {

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 处理器标识。
     */
    private String handlerKey;

    /**
     * Solon Bean 名称。
     */
    private String beanName;

    /**
     * Bean 类名。
     */
    private String className;

    /**
     * 方法名称。
     */
    private String methodName;

    /**
     * 处理器说明。
     */
    private String description;
}
