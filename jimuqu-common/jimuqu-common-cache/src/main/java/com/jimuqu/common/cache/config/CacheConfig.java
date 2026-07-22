package com.jimuqu.common.cache.config;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.data.cache.CacheServiceSupplier;
import org.redisson.api.RedissonClient;

import java.io.Closeable;
import java.util.Objects;
import java.util.Properties;

@Configuration
@Slf4j
public class CacheConfig {

    /**
     * 根据配置加载缓存服务
     *
     * @return 缓存服务
     */
    @Bean(typed = true)
    public CacheService cacheService() {
        Properties properties = Solon.cfg().getProp("jimuqu.cache");
        removeBlankCredential(properties, "user");
        removeBlankCredential(properties, "password");

        CacheService cacheService = new CacheServiceSupplier(properties).get();
        log.info("Cache: {}", cacheService.getClass().getSimpleName());
        return cacheService;
    }

    /**
     * 将 Redisson 客户端绑定到当前 Solon 上下文的生命周期。
     */
    @Bean
    @Condition(onBean = RedissonCacheService.class)
    public Closeable redissonClientCloser(
            @Inject RedissonCacheService cacheService) {
        RedissonClient client = Objects.requireNonNull(
                cacheService.client(), "RedissonClient 不能为空");
        return () -> {
            if (!client.isShutdown() && !client.isShuttingDown()) {
                client.shutdown();
            }
        };
    }

    static void removeBlankCredential(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value != null && value.isBlank()) {
            properties.remove(name);
        }
    }

}
