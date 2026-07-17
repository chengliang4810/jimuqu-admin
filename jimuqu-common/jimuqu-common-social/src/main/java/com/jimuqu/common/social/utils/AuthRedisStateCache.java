package com.jimuqu.common.social.utils;

import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.common.core.utils.StringUtil;
import lombok.AllArgsConstructor;
import me.zhyd.oauth.cache.AuthStateCache;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.cache.CacheService;

/**
 * 授权状态缓存
 */
@AllArgsConstructor
@Component
public class AuthRedisStateCache implements AuthStateCache {

    @Inject
    private CacheService cacheService;

    /**
     * 存入缓存
     *
     * @param key   缓存key
     * @param value 缓存内容
     */
    @Override
    public void cache(String key, String value) {
        // 授权超时时间 默认三分钟
        cacheService.store(GlobalConstants.SOCIAL_AUTH_CODE_KEY + key, value, 3 * 60);
    }

    /**
     * 存入缓存
     *
     * @param key     缓存key
     * @param value   缓存内容
     * @param timeout 指定缓存过期时间(毫秒)
     */
    @Override
    public void cache(String key, String value, long timeout) {
        cacheService.store(GlobalConstants.SOCIAL_AUTH_CODE_KEY + key, value, Math.toIntExact(timeout / 1000));
    }

    /**
     * 获取缓存内容
     *
     * @param key 缓存key
     * @return 缓存内容
     */
    @Override
    public String get(String key) {
        return cacheService.get(GlobalConstants.SOCIAL_AUTH_CODE_KEY + key, String.class);
    }

    /**
     * 是否存在key，如果对应key的value值已过期，也返回false
     *
     * @param key 缓存key
     * @return true：存在key，并且value没过期；false：key不存在或者已过期
     */
    @Override
    public boolean containsKey(String key) {
        String string = cacheService.get(GlobalConstants.SOCIAL_AUTH_CODE_KEY + key, String.class);
        return StringUtil.isNotBlank(string);
    }
}
