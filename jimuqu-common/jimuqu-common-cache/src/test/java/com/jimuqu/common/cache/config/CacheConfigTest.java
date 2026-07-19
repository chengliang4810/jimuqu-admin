package com.jimuqu.common.cache.config;

import org.junit.jupiter.api.Test;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.solon.core.AppContext;
import org.redisson.api.RedissonClient;

import java.io.Closeable;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheConfigTest {

    @Test
    void commonConfigDoesNotOverrideApplicationCacheNamespace() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/common-cache.yml")) {
            assertNotNull(input);
            String config = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(config.contains("keyHeader:"));
        }
    }

    @Test
    void removesBlankRedisCredentialsBeforeCreatingRedissonClient() {
        Properties properties = new Properties();
        properties.setProperty("user", "  ");
        properties.setProperty("password", "");

        CacheConfig.removeBlankCredential(properties, "user");
        CacheConfig.removeBlankCredential(properties, "password");

        assertFalse(properties.containsKey("user"));
        assertFalse(properties.containsKey("password"));
    }

    @Test
    void keepsConfiguredRedisCredentials() {
        Properties properties = new Properties();
        properties.setProperty("password", "secret");

        CacheConfig.removeBlankCredential(properties, "password");

        assertEquals("secret", properties.getProperty("password"));
    }

    @Test
    void closesTheRedissonClientWithEachSolonContext() {
        CacheConfig config = new CacheConfig();
        AtomicBoolean firstStopped = new AtomicBoolean();
        AtomicInteger firstShutdowns = new AtomicInteger();
        TestAppContext firstContext = context(
                config.redissonClientCloser(cacheService(client(firstStopped, firstShutdowns))));

        firstContext.closeBeans();
        assertTrue(firstStopped.get());
        assertEquals(1, firstShutdowns.get());

        AtomicBoolean secondStopped = new AtomicBoolean();
        AtomicInteger secondShutdowns = new AtomicInteger();
        TestAppContext secondContext = context(
                config.redissonClientCloser(cacheService(client(secondStopped, secondShutdowns))));

        secondContext.closeBeans();
        assertTrue(secondStopped.get());
        assertEquals(1, secondShutdowns.get());
    }

    private static TestAppContext context(Closeable closer) {
        TestAppContext context = new TestAppContext();
        context.wrapAndPut(Closeable.class, closer, true);
        return context;
    }

    private static RedissonCacheService cacheService(RedissonClient client) {
        return new RedissonCacheService(client, "test", 30);
    }

    private static RedissonClient client(AtomicBoolean stopped, AtomicInteger shutdowns) {
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(),
                new Class<?>[]{RedissonClient.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "shutdown" -> {
                        shutdowns.incrementAndGet();
                        stopped.set(true);
                        yield null;
                    }
                    case "isShutdown" -> stopped.get();
                    case "isShuttingDown" -> false;
                    case "toString" -> "RedissonClientProxy";
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

    private static final class TestAppContext extends AppContext {

        private void closeBeans() {
            beanStop0();
        }
    }
}
