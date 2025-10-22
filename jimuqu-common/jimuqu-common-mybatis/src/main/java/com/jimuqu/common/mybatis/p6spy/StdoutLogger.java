package com.jimuqu.common.mybatis.p6spy;

/**
 * 输出 SQL 日志到控制台
 *
 * @author chengliang4810
 * @since 2025/05/23
 */
public class StdoutLogger extends com.p6spy.engine.spy.appender.StdoutLogger {

    @Override
    public void logText(String text) {
        // 打印红色 SQL 日志
        System.err.println(text);
    }

}