package com.jimuqu.common.cache;

import org.noear.solon.data.cache.CacheService;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 通过版本隔离实现缓存命名空间整体失效。
 */
public final class VersionedCacheNamespace {

    private static final String INITIAL_VERSION = "0";
    private static final String VERSION_KEY_SUFFIX = "__namespace_version";
    private static final int VERSION_TTL_SECONDS = Integer.MAX_VALUE;

    private final CacheService cacheService;
    private final String namespacePrefix;
    private final String versionKey;

    public VersionedCacheNamespace(CacheService cacheService, String namespace) {
        this.cacheService = Objects.requireNonNull(cacheService, "cacheService 不能为空");
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("缓存命名空间不能为空");
        }
        this.namespacePrefix = namespace.endsWith(":") ? namespace : namespace + ":";
        this.versionKey = namespacePrefix + VERSION_KEY_SUFFIX;
    }

    public <T> T getOrStore(String key, Type type, int seconds, Supplier<T> supplier) {
        return cacheService.getOrStore(versionedKey(key), type, seconds, supplier);
    }

    public <T> T get(String key, Type type) {
        return cacheService.get(versionedKey(key), type);
    }

    public void store(String key, Object value, int seconds) {
        cacheService.store(versionedKey(key), value, seconds);
    }

    public void remove(String key) {
        cacheService.remove(versionedKey(key));
    }

    /**
     * 推进命名空间版本，使此前的全部缓存项立即不可达。
     */
    public void refresh() {
        String previousVersion = currentVersion();
        String nextVersion = UUID.randomUUID().toString();
        cacheService.store(versionKey, nextVersion, VERSION_TTL_SECONDS);
        String activeVersion = currentVersion();
        if (Objects.equals(previousVersion, activeVersion)) {
            throw new IllegalStateException("缓存命名空间刷新失败: " + namespacePrefix);
        }
    }

    private String versionedKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("缓存键不能为空");
        }
        return namespacePrefix + currentVersion() + ":" + key;
    }

    private String currentVersion() {
        String version = cacheService.get(versionKey, String.class);
        return version == null || version.isBlank() ? INITIAL_VERSION : version;
    }
}
