package com.jimuqu.common.idempotent.validation;

import org.junit.jupiter.api.Test;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoRepeatSubmitCheckerImplTest {

    @Test
    void reservesAtomicallyForLocalCache() throws Exception {
        MemoryCache cache = new MemoryCache();
        NoRepeatSubmitCheckerImpl checker = new NoRepeatSubmitCheckerImpl(cache);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return checker.tryReserve("same-request", 5);
                }));
            }
            start.countDown();
            assertEquals(1, futures.stream().filter(NoRepeatSubmitCheckerImplTest::result).count());
        } finally {
            executor.shutdownNow();
        }
        assertEquals(5, cache.seconds.get("same-request"));
    }

    @Test
    void releasesFailedRequestButRetainsSuccessfulRequest() throws Exception {
        MemoryCache cache = new MemoryCache();
        NoRepeatSubmitCheckerImpl checker = new NoRepeatSubmitCheckerImpl(cache);
        NoRepeatSubmit annotation = annotation("defaultSubmit");
        TestContext context = new TestContext();

        assertTrue(checker.check(annotation, context, "hash", 1));
        checker.complete(false);
        assertTrue(checker.check(annotation, context, "hash", 1));
        checker.complete(true);
        assertFalse(checker.check(annotation, context, "hash", 1));
    }

    @Test
    void mapsOnlySolonDefaultsToBellFiveSecondWindow() throws Exception {
        NoRepeatSubmit defaults = annotation("defaultSubmit");
        NoRepeatSubmit explicit = annotation("explicitSubmit");

        assertEquals(5, NoRepeatSubmitCheckerImpl.effectiveSeconds(defaults, defaults.seconds()));
        assertEquals(2, NoRepeatSubmitCheckerImpl.effectiveSeconds(explicit, explicit.seconds()));
    }

    @Test
    void keepsRedissonReservationInsideConfiguredNamespace() {
        String key = "global:repeat_submit:POST:/system/user:hash";
        String keyHeader = "jimu:it:run-id:";
        ExposedRedissonCacheService cacheService = new ExposedRedissonCacheService(null, keyHeader);

        String redisKey = NoRepeatSubmitCheckerImpl.redissonCacheKey(key, keyHeader);

        assertEquals(cacheService.physicalKey(key), redisKey);
        assertTrue(redisKey.startsWith(keyHeader + ":"));
        assertFalse(redisKey.contains(key));
    }

    @Test
    void redissonFailureReleasesPhysicalKeyAndSuccessRetainsFiveSecondWindow() throws Exception {
        String keyHeader = "jimu:it:repeat-test:";
        RedissonFixture fixture = new RedissonFixture();
        ExposedRedissonCacheService cacheService = new ExposedRedissonCacheService(fixture.client, keyHeader);
        NoRepeatSubmitCheckerImpl checker = new NoRepeatSubmitCheckerImpl(cacheService, keyHeader);
        NoRepeatSubmit annotation = annotation("defaultSubmit");
        TestContext context = new TestContext();

        assertTrue(checker.check(annotation, context, "same-hash", 1));
        String physicalKey = fixture.lastSetKey;
        assertEquals(Duration.ofSeconds(5), fixture.expirations.get(physicalKey));
        checker.complete(false);
        assertEquals(physicalKey, fixture.lastDeleteKey);
        assertFalse(fixture.values.containsKey(physicalKey));

        assertTrue(checker.check(annotation, context, "same-hash", 1));
        checker.complete(true);
        assertTrue(fixture.values.containsKey(physicalKey));
        assertFalse(checker.check(annotation, context, "same-hash", 1));
    }

    @Test
    void preservesCustomAnnotationMillisecondWindowInRedis() {
        RedissonFixture fixture = new RedissonFixture();
        ExposedRedissonCacheService cacheService = new ExposedRedissonCacheService(
                fixture.client, "jimu:it:repeat-test:");
        NoRepeatSubmitCheckerImpl checker = new NoRepeatSubmitCheckerImpl(
                cacheService, "jimu:it:repeat-test:");

        assertTrue(checker.tryReserve("custom-window", Duration.ofMillis(1500)));

        assertEquals(Duration.ofMillis(1500), fixture.expirations.get(fixture.lastSetKey));
    }

    private static boolean result(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static NoRepeatSubmit annotation(String method) throws NoSuchMethodException {
        return NoRepeatSubmitCheckerImplTest.class.getDeclaredMethod(method).getAnnotation(NoRepeatSubmit.class);
    }

    @NoRepeatSubmit
    private void defaultSubmit() {
    }

    @NoRepeatSubmit(seconds = 2, message = "custom")
    private void explicitSubmit() {
    }

    private static final class TestContext extends ContextEmpty {
        @Override
        public String method() {
            return "POST";
        }

        @Override
        public String url() {
            return "/system/test";
        }
    }

    private static final class MemoryCache implements CacheService {
        private final Map<String, Object> values = new ConcurrentHashMap<>();
        private final Map<String, Integer> seconds = new ConcurrentHashMap<>();

        @Override
        public void store(String key, Object value, int seconds) {
            values.put(key, value);
            this.seconds.put(key, seconds);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
            seconds.remove(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key, Type type) {
            return (T) values.get(key);
        }
    }

    private static final class ExposedRedissonCacheService extends RedissonCacheService {
        private ExposedRedissonCacheService(RedissonClient client, String keyHeader) {
            super(client, keyHeader, 30);
            enableMd5key(true);
        }

        private String physicalKey(String logicalKey) {
            return newKey(logicalKey);
        }
    }

    private static final class RedissonFixture {
        private final Map<String, Object> values = new ConcurrentHashMap<>();
        private final Map<String, Duration> expirations = new ConcurrentHashMap<>();
        private volatile String lastSetKey;
        private volatile String lastDeleteKey;
        private final RedissonClient client = proxy(RedissonClient.class, (method, args) -> {
            if ("getBucket".equals(method.getName())) {
                return bucket(String.valueOf(args[0]));
            }
            return defaultValue(method.getReturnType());
        });

        private RBucket<Object> bucket(String key) {
            return proxy(RBucket.class, (method, args) -> switch (method.getName()) {
                case "setIfAbsent" -> {
                    lastSetKey = key;
                    if (args.length > 1 && args[1] instanceof Duration duration) {
                        expirations.put(key, duration);
                    }
                    yield values.putIfAbsent(key, args[0]) == null;
                }
                case "delete" -> {
                    lastDeleteKey = key;
                    expirations.remove(key);
                    yield values.remove(key) != null;
                }
                case "get" -> values.get(key);
                case "getName" -> key;
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
            return 0;
        }

        @FunctionalInterface
        private interface Invocation {
            Object invoke(java.lang.reflect.Method method, Object[] args) throws Throwable;
        }
    }
}
