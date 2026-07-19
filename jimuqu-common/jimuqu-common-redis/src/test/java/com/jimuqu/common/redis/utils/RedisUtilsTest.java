package com.jimuqu.common.redis.utils;

import org.junit.jupiter.api.Test;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.solon.core.AppContext;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisUtilsTest {

    @Test
    void publishAllowsTheOptionalCallbackToBeNull() {
        AtomicBoolean published = new AtomicBoolean();
        RTopic topic = proxy(RTopic.class, (method, args) -> {
            if ("publish".equals(method.getName())) {
                published.set(true);
            }
            return defaultValue(method.getReturnType());
        });
        RedissonClient client = proxy(RedissonClient.class, (method, args) ->
                "getTopic".equals(method.getName()) ? topic : defaultValue(method.getReturnType()));

        RedisUtils.publish(client, "message", "payload", null);

        assertTrue(published.get());
    }

    @Test
    void keepTtlFallbackTreatsMissingKeysAsPersistentWrites() {
        AtomicBoolean plainSet = new AtomicBoolean();
        RBucket<Object> bucket = proxy(RBucket.class, (method, args) -> switch (method.getName()) {
            case "setAndKeepTTL" -> throw new IllegalStateException("Redis 5");
            case "remainTimeToLive" -> -2L;
            case "set" -> {
                if (args.length == 1) {
                    plainSet.set(true);
                }
                yield null;
            }
            default -> defaultValue(method.getReturnType());
        });
        RedissonClient client = proxy(RedissonClient.class, (method, args) ->
                "getBucket".equals(method.getName()) ? bucket : defaultValue(method.getReturnType()));

        RedisUtils.setCacheObject(client, "missing", "value", true);

        assertTrue(plainSet.get());
    }

    @Test
    void deletingANullKeyDoesNotTouchRedis() {
        AtomicBoolean bucketRequested = new AtomicBoolean();
        RedissonClient client = proxy(RedissonClient.class, (method, args) -> {
            if ("getBucket".equals(method.getName())) {
                bucketRequested.set(true);
            }
            return defaultValue(method.getReturnType());
        });

        assertFalse(RedisUtils.deleteObject(client, null));
        assertFalse(bucketRequested.get());
    }

    @Test
    void resolvesTheActiveClientFromEachContextWithoutReusingTheStoppedClient() {
        AtomicBoolean firstStopped = new AtomicBoolean();
        RedissonClient first = client(firstStopped);
        AppContext firstContext = context(first);

        assertSame(first, RedisUtils.getClient(firstContext));
        firstStopped.set(true);
        assertThrows(IllegalStateException.class, () -> RedisUtils.getClient(firstContext));

        RedissonClient second = client(new AtomicBoolean());
        assertSame(second, RedisUtils.getClient(context(second)));
    }

    private static AppContext context(RedissonClient client) {
        AppContext context = new AppContext();
        context.wrapAndPut(RedissonCacheService.class,
                new RedissonCacheService(client, "test", 30), true);
        return context;
    }

    private static RedissonClient client(AtomicBoolean stopped) {
        return proxy(RedissonClient.class, (method, args) -> switch (method.getName()) {
            case "isShutdown" -> stopped.get();
            case "isShuttingDown" -> false;
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return invocation.invoke(method, args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        return 0;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] args) throws Throwable;
    }
}
