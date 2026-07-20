package com.jimuqu.common.cache;

import org.junit.jupiter.api.Test;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.data.cache.LocalCacheService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VersionedCacheNamespaceTest {

    @Test
    void refreshMakesOldLocalCacheEntriesUnreachable() {
        LocalCacheService cacheService = new LocalCacheService(3600);
        try {
            assertRefreshMakesOldEntryUnreachable(cacheService);
        } finally {
            cacheService.clear();
        }
    }

    @Test
    void refreshMakesOldMd5RedissonCacheEntriesUnreachable() {
        assertRefreshMakesOldEntryUnreachable(redissonCacheService());
    }

    private static void assertRefreshMakesOldEntryUnreachable(CacheService cacheService) {
        VersionedCacheNamespace namespace = new VersionedCacheNamespace(cacheService, "sys_dict");
        namespace.store("deleted-type", "stale", 3600);
        assertEquals("stale", namespace.get("deleted-type", String.class));

        namespace.refresh();

        assertNull(namespace.get("deleted-type", String.class));
        namespace.store("deleted-type", "fresh", 3600);
        assertEquals("fresh", namespace.get("deleted-type", String.class));
    }

    private static RedissonCacheService redissonCacheService() {
        Map<String, Object> values = new ConcurrentHashMap<>();
        RedissonClient client = (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(), new Class<?>[]{RedissonClient.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBucket" -> bucket(values, (String) args[0]);
                    case "isShutdown", "isShuttingDown" -> false;
                    case "toString" -> "RedissonClientProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
        return new RedissonCacheService(client, "test", 3600);
    }

    @SuppressWarnings("unchecked")
    private static RBucket<Object> bucket(Map<String, Object> values, String key) {
        return (RBucket<Object>) Proxy.newProxyInstance(
                RBucket.class.getClassLoader(), new Class<?>[]{RBucket.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "set" -> {
                        values.put(key, args[0]);
                        yield null;
                    }
                    case "get" -> values.get(key);
                    case "delete" -> values.remove(key) != null;
                    case "toString" -> "RBucketProxy(" + key + ")";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
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
        return 0;
    }
}
