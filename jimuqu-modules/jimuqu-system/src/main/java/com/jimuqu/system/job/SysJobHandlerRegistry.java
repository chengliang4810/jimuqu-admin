package com.jimuqu.system.job;

import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.system.domain.SysJob;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.BeanWrap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时任务处理器白名单注册表
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Slf4j
@Component
public class SysJobHandlerRegistry {

    private final Map<String, HandlerMethod> handlerMap = new ConcurrentHashMap<>();

    /**
     * 扫描 Solon 容器中标注 {@link SysJobHandler} 的方法。
     */
    public synchronized void refresh() {
        Map<String, HandlerMethod> scanned = new LinkedHashMap<>();
        Solon.context().beanForeach((beanName, beanWrap) -> scanBean(beanName, beanWrap, scanned));
        handlerMap.clear();
        handlerMap.putAll(scanned);
        log.info("定时任务处理器扫描完成，共注册 {} 个", handlerMap.size());
    }

    /**
     * 获取已注册处理器，永远不返回null。
     */
    public Map<String, HandlerMethod> getHandlerMap() {
        return Collections.unmodifiableMap(handlerMap);
    }

    /**
     * 获取处理器列表。
     */
    public List<SysJobHandlerVo> listHandlers() {
        List<SysJobHandlerVo> list = new ArrayList<>();
        for (HandlerMethod handlerMethod : handlerMap.values()) {
            list.add(handlerMethod.toVo());
        }
        list.sort(Comparator.comparing(SysJobHandlerVo::getHandlerKey));
        return list;
    }

    /**
     * 判断处理器是否存在。
     */
    public boolean contains(String handlerKey) {
        return handlerMap.containsKey(handlerKey);
    }

    /**
     * 执行处理器。
     */
    public void invoke(SysJob job) throws Throwable {
        if (job == null || StringUtil.isBlank(job.getHandlerKey())) {
            throw new IllegalArgumentException("任务处理器标识不能为空");
        }
        HandlerMethod handlerMethod = handlerMap.get(job.getHandlerKey());
        if (handlerMethod == null) {
            throw new IllegalArgumentException("未找到定时任务处理器: " + job.getHandlerKey());
        }
        handlerMethod.invoke(job);
    }

    private void scanBean(String beanName, BeanWrap beanWrap, Map<String, HandlerMethod> scanned) {
        if (beanWrap == null || beanWrap.clz() == null) {
            return;
        }
        Object bean = beanWrap.raw();
        if (bean == null) {
            return;
        }
        for (Method method : beanWrap.clz().getMethods()) {
            SysJobHandler annotation = method.getAnnotation(SysJobHandler.class);
            if (annotation == null || method.isBridge() || method.isSynthetic()) {
                continue;
            }
            validateHandlerMethod(annotation, method);
            HandlerMethod handlerMethod = new HandlerMethod(annotation.value(), annotation.name(), beanName, bean, method);
            HandlerMethod old = scanned.putIfAbsent(annotation.value(), handlerMethod);
            if (old != null) {
                throw new IllegalStateException("定时任务处理器标识重复: " + annotation.value());
            }
        }
    }

    private void validateHandlerMethod(SysJobHandler annotation, Method method) {
        if (StringUtil.isBlank(annotation.value())) {
            throw new IllegalStateException("定时任务处理器标识不能为空: " + method);
        }
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("定时任务处理器不支持静态方法: " + method);
        }
        int parameterCount = method.getParameterCount();
        if (parameterCount > 1) {
            throw new IllegalStateException("定时任务处理器最多只能声明一个参数: " + method);
        }
        if (parameterCount == 1) {
            Class<?> parameterType = method.getParameterTypes()[0];
            boolean supported = SysJobContext.class.isAssignableFrom(parameterType)
                    || String.class.equals(parameterType)
                    || Map.class.isAssignableFrom(parameterType);
            if (!supported) {
                throw new IllegalStateException("定时任务处理器参数类型仅支持 SysJobContext、String、Map: " + method);
            }
        }
    }

    /**
     * 已注册处理器方法。
     */
    public static class HandlerMethod {
        private final String handlerKey;
        private final String handlerName;
        private final String beanName;
        private final Object bean;
        private final Method method;

        HandlerMethod(String handlerKey, String handlerName, String beanName, Object bean, Method method) {
            this.handlerKey = handlerKey;
            this.handlerName = StringUtil.defaultIfBlank(handlerName, handlerKey);
            this.beanName = beanName;
            this.bean = bean;
            this.method = method;
            this.method.setAccessible(true);
        }

        public SysJobHandlerVo toVo() {
            return new SysJobHandlerVo()
                    .setHandlerKey(handlerKey)
                    .setHandlerName(handlerName)
                    .setBeanName(beanName)
                    .setMethodName(method.getName());
        }

        public void invoke(SysJob job) throws Throwable {
            try {
                if (method.getParameterCount() == 0) {
                    method.invoke(bean);
                } else {
                    method.invoke(bean, buildArgument(method.getParameterTypes()[0], job));
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        private Object buildArgument(Class<?> parameterType, SysJob job) {
            SysJobContext context = new SysJobContext(job);
            if (SysJobContext.class.isAssignableFrom(parameterType)) {
                return context;
            }
            if (String.class.equals(parameterType)) {
                return context.getParamJson();
            }
            return context.getParams();
        }
    }
}
