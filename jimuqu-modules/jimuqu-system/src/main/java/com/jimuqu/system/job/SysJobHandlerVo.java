package com.jimuqu.system.job;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 定时任务处理器视图对象
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Data
@Accessors(chain = true)
public class SysJobHandlerVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 处理器标识
     */
    private String handlerKey;

    /**
     * 处理器名称
     */
    private String handlerName;

    /**
     * 所属Bean
     */
    private String beanName;

    /**
     * 方法名
     */
    private String methodName;
}
