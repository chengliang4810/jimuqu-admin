package com.jimuqu.system.service;

import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.system.domain.vo.ScheduledJobHandlerVo;
import com.jimuqu.system.task.ScheduledJobHandler;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.BeanWrap;
import org.noear.solon.core.handle.Context;
import org.noear.solon.scheduling.scheduled.JobHandler;
import org.noear.solon.scheduling.scheduled.proxy.JobHandlerMethodProxy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 在线定时任务处理器白名单注册表。
 */
@Component
public class ScheduledJobHandlerRegistry {

    /**
     * 处理器标识格式。
     */
    private static final Pattern HANDLER_KEY =
            Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,127}");

    /**
     * 当前已扫描的不可变处理器表。
     */
    private volatile Map<String, HandlerMethod> handlers = Map.of();

    /**
     * 重新扫描 Solon Bean 中显式允许的方法。
     */
    public synchronized void refresh() {
        Map<String, HandlerMethod> discovered = new LinkedHashMap<>();
        Solon.context().beanForeach(beanWrap -> registerBean(discovered, beanWrap));
        handlers = Map.copyOf(discovered);
    }

    /**
     * 获取界面可选择的处理器。
     *
     * @return 处理器列表
     */
    public List<ScheduledJobHandlerVo> list() {
        ensureScanned();
        return handlers.values().stream()
                .map(handler -> new ScheduledJobHandlerVo(
                        handler.key(), handler.beanName(), handler.className(),
                        handler.methodName(), handler.description()))
                .sorted(Comparator.comparing(ScheduledJobHandlerVo::getHandlerKey))
                .toList();
    }

    /**
     * 校验处理器是否属于白名单。
     *
     * @param handlerKey 处理器标识
     */
    public void require(String handlerKey) {
        ensureScanned();
        if (!handlers.containsKey(handlerKey)) {
            throw new ServiceException("定时任务处理器不在白名单中: " + handlerKey);
        }
    }

    /**
     * 调用白名单处理器。
     *
     * @param handlerKey 处理器标识
     * @param context 当前任务上下文
     * @throws Throwable 处理器执行异常
     */
    public void invoke(String handlerKey, Context context) throws Throwable {
        ensureScanned();
        HandlerMethod handler = handlers.get(handlerKey);
        if (handler == null) {
            throw new ServiceException("定时任务处理器不在白名单中: " + handlerKey);
        }
        handler.handler().handle(context);
    }

    /**
     * 首次使用时完成扫描。
     */
    private void ensureScanned() {
        if (handlers.isEmpty()) {
            refresh();
        }
    }

    /**
     * 扫描单个 Solon Bean。
     *
     * @param discovered 已发现处理器
     * @param beanWrap Solon Bean 包装
     */
    private static void registerBean(
            Map<String, HandlerMethod> discovered, BeanWrap beanWrap) {
        for (Method method : beanWrap.clz().getMethods()) {
            ScheduledJobHandler annotation = method.getAnnotation(ScheduledJobHandler.class);
            if (annotation == null) {
                continue;
            }
            validateMethod(method, annotation);
            HandlerMethod handler = new HandlerMethod(
                    annotation.key(), beanWrap.name(), beanWrap.clz().getName(),
                    method.getName(), annotation.description(), beanWrap, method,
                    new JobHandlerMethodProxy(beanWrap, method));
            HandlerMethod previous = discovered.putIfAbsent(annotation.key(), handler);
            if (previous != null
                    && (previous.beanWrap() != beanWrap
                    || !previous.method().equals(method))) {
                throw new IllegalStateException(
                        "定时任务处理器标识重复: " + annotation.key());
            }
        }
    }

    /**
     * 校验白名单方法签名。
     *
     * @param method 方法
     * @param annotation 白名单注解
     */
    static void validateMethod(Method method, ScheduledJobHandler annotation) {
        if (!HANDLER_KEY.matcher(annotation.key()).matches()) {
            throw new IllegalStateException(
                    "定时任务处理器标识格式错误: " + annotation.key());
        }
        if (annotation.description().isBlank()
                || annotation.description().length() > 200) {
            throw new IllegalStateException(
                    "定时任务处理器说明不能为空且不能超过200个字符: " + annotation.key());
        }
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers)
                || Modifier.isStatic(modifiers)
                || method.getParameterCount() != 0
                || method.getReturnType() != void.class) {
            throw new IllegalStateException(
                    "定时任务处理器必须是 public 非 static 无参且返回 void 的方法: "
                            + annotation.key());
        }
    }

    /**
     * 已校验的处理器方法。
     *
     * @param key 处理器标识
     * @param beanName Solon Bean 名称
     * @param className Bean 类名
     * @param methodName 方法名称
     * @param description 处理器说明
     * @param beanWrap Solon Bean 包装
     * @param method 白名单方法
     * @param handler 走 Solon 方法切面的任务处理器
     */
    private record HandlerMethod(
            String key, String beanName, String className, String methodName,
            String description, BeanWrap beanWrap, Method method,
            JobHandler handler) {
    }
}
