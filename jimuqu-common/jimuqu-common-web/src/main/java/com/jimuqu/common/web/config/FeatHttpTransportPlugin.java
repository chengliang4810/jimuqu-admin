package com.jimuqu.common.web.config;

import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.event.EventListener;
import org.noear.solon.server.prop.impl.HttpServerProps;
import tech.smartboot.feat.core.server.ServerOptions;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Windows 下使用 JDK AIO，规避 SmartSocket 增强通道在异步响应压测中出现的连接停滞。
 * 保留 FeatHTTP、业务线程池和长连接；其他平台默认沿用容器通道。
 */
public final class FeatHttpTransportPlugin implements Plugin {
    private final List<AsynchronousChannelGroup> groups = new ArrayList<>();
    private EventListener<ServerOptions> listener;

    @Override
    public void start(AppContext context) {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
        if (!context.cfg().getBool("server.http.useJdkAio", windows)) {
            return;
        }
        listener = options -> {
            // 尊重其他扩展已经显式提供的通道组。
            if (options.group() != null) {
                return;
            }
            int threads = Math.max(1, new HttpServerProps().getCoreThreads());
            AtomicInteger sequence = new AtomicInteger();
            AsynchronousChannelGroup group = AsynchronousChannelGroup.withFixedThreadPool(threads, task -> {
                Thread thread = new Thread(task, "feat-jdk-aio-" + sequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
            groups.add(group);
            options.group(group);
        };
        EventBus.subscribe(ServerOptions.class, listener);
    }

    @Override
    public void stop() throws Exception {
        if (listener != null) {
            EventBus.unsubscribe(listener);
        }
        try {
            for (AsynchronousChannelGroup group : groups) {
                group.shutdownNow();
            }
        } finally {
            groups.clear();
        }
    }
}
