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

/**
 * 限流拦截器
 */
@Slf4j
@Component(index = -80)
public class RateLimitFilter implements RouterInterceptor {

    @Inject
    private RateLimiter rateLimiter;

    @Inject
    private RateLimitConfig globalConfig;

    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {
        try {
            if (!globalConfig.isEnabled()) {
                chain.doIntercept(ctx, mainHandler);
                return;
            }
            // 获取当前请求的控制器方法
            Action action = mainHandler instanceof Action handlerAction ? handlerAction : ctx.action();
            Method method = action == null ? null : action.method().getMethod();
            if (method == null) {
                chain.doIntercept(ctx, mainHandler);
                return;
            }

            // 获取限流注解
            RateLimit rateLimit = method.getAnnotation(RateLimit.class);
            if (rateLimit == null && action.controller() != null) {
                rateLimit = action.controller().clz().getAnnotation(RateLimit.class);
            }
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
            throw e;
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
     * 创建限流配置
     */
    static RateLimitConfig createRateLimitConfig(RateLimit rateLimit) {
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
    }



}
