package com.jimuqu.common.ratelimit.annotation;

import cn.hutool.v7.core.text.StrUtil;
import com.jimuqu.common.ratelimit.core.RateLimitConfig;
import com.jimuqu.common.ratelimit.core.RateLimiter;
import com.jimuqu.common.ratelimit.enums.RateLimitType;
import com.jimuqu.common.ratelimit.exception.RateLimitException;
import com.jimuqu.common.ratelimit.utils.RateLimitUtils;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Action;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流拦截器
 */
@Slf4j
@Component(index = -999)
public class RateLimitInterceptor implements RouterInterceptor {

    @Inject
    private RateLimiter rateLimiter;

    @Inject
    private RateLimitConfig globalConfig;

    private final ConcurrentHashMap<String, RateLimitConfig> configCache = new ConcurrentHashMap<>();


    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {
        try {
            // 获取当前请求的控制器方法
            Method method = getHandlerMethod(ctx);
            if (method == null) {
                chain.doIntercept(ctx, mainHandler);
                return;
            }

            // 获取限流注解
            RateLimit rateLimit = method.getAnnotation(RateLimit.class);
            if (rateLimit == null) {
                chain.doIntercept(ctx, mainHandler);
                return;
            }

            if (!rateLimit.enabled()) {
                chain.doIntercept(ctx, mainHandler);
                return;
            }

            // 构建限流键
            String key = buildRateLimitKey(method, rateLimit, ctx);

            // 创建限流配置
            RateLimitConfig config = createRateLimitConfig(rateLimit);
            // 设置键前缀
            config.setKeyPrefix(globalConfig.getKeyPrefix());

            // 尝试获取令牌
            boolean acquired = rateLimiter.tryAcquire(key, 1, config);

            if (!acquired) {
                String message = rateLimit.message();
                if (StrUtil.isEmpty(message)) {
                    message = config.getErrorMessage();
                }
                throw new RateLimitException(message);
            }

            chain.doIntercept(ctx, mainHandler);

        } catch (RateLimitException e) {
            log.warn("限流异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("限流拦截器异常 - Path: {}, 异常: {}", ctx.path(), e.getMessage(), e);
            // 限流器异常时，默认放行
            chain.doIntercept(ctx, mainHandler);
        }
    }


    /**
     * 构建限流键
     */
    private String buildRateLimitKey(Method method, RateLimit rateLimit, Context ctx) {
        String ip = null;
        Long userId = null;

        if (rateLimit.type() == RateLimitType.IP) {
            ip = RateLimitUtils.getClientIp(ctx);
        } else if (rateLimit.type() == RateLimitType.USER) {
            userId = RateLimitUtils.getCurrentUserId();
        }

        return RateLimitUtils.buildRateLimitKey(method, rateLimit.key(), ip, userId, rateLimit.type());
    }

    /**
     * 获取当前处理的方法
     */
    private Method getHandlerMethod(Context ctx) {
        try {
            // 从上下文中获取当前处理的方法信息
            Action action = ctx.action();
            if (action == null) {
                return null;
            }
            return action.method().getMethod();
        } catch (Exception e) {
            log.warn("获取处理器方法失败", e);
            return null;
        }
    }

    /**
     * 创建限流配置
     */
    private RateLimitConfig createRateLimitConfig(RateLimit rateLimit) {
        String cacheKey = rateLimit.type() + ":" + rateLimit.permitsPerSecond() + ":" + rateLimit.maxBurst();

        return configCache.computeIfAbsent(cacheKey, k -> {
            RateLimitConfig config = new RateLimitConfig();
            config.setType(rateLimit.type());
            config.setPermitsPerSecond(rateLimit.permitsPerSecond());
            config.setMaxBurst(rateLimit.maxBurst());
            config.setWindow(rateLimit.window());
            config.setAlgorithm(rateLimit.algorithm());
            config.setEnabled(rateLimit.enabled());

            if (StrUtil.isNotEmpty(rateLimit.message())) {
                config.setErrorMessage(rateLimit.message());
            }

            return config;
        });
    }


}