package com.jimuqu.common.satoken.core;

import cn.dev33.satoken.dao.SaTokenDaoForRedisson;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.auto.SaTokenDaoByObjectFollowString;
import org.redisson.api.RedissonClient;

import java.util.List;

/**
 * 为 Sa-Token 的原始 Redisson 键补充项目级命名空间。
 */
public class PrefixedSaTokenDaoForRedisson implements SaTokenDaoByObjectFollowString, SaTokenDao {

    private final SaTokenDaoForRedisson delegate;
    private final String keyPrefix;

    public PrefixedSaTokenDaoForRedisson(RedissonClient redissonClient, String keyPrefix) {
        this.delegate = new SaTokenDaoForRedisson(redissonClient);
        this.keyPrefix = normalizePrefix(keyPrefix);
    }

    @Override
    public String get(String key) {
        return delegate.get(prefixed(key));
    }

    @Override
    public void set(String key, String value, long timeout) {
        delegate.set(prefixed(key), value, timeout);
    }

    @Override
    public void update(String key, String value) {
        delegate.update(prefixed(key), value);
    }

    @Override
    public void delete(String key) {
        delegate.delete(prefixed(key));
    }

    @Override
    public long getTimeout(String key) {
        return delegate.getTimeout(prefixed(key));
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        delegate.updateTimeout(prefixed(key), timeout);
    }

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        return delegate.searchData(prefixed(prefix), keyword, start, size, sortType);
    }

    private String prefixed(String key) {
        return keyPrefix + key;
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String normalized = prefix.trim();
        return normalized.endsWith(":") ? normalized : normalized + ":";
    }
}
