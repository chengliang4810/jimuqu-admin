package com.jimuqu.common.idempotent.validation;

import cn.dev33.satoken.SaManager;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.hutool.v7.crypto.SecureUtil;
import com.jimuqu.common.core.constant.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.cache.redisson.RedissonCacheService;
import org.noear.solon.core.handle.Context;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NoRepeatSubmitChecker;
import org.noear.solon.validation.ValidatorManager;

import java.time.Duration;

/**
 * 重复提交检查器
 *
 * @author chengliang
 * @date 2025/12/14
 */
@Slf4j
@Component
public class NoRepeatSubmitCheckerImpl implements NoRepeatSubmitChecker {

    private static final int BELL_DEFAULT_SECONDS = 5;
    private static final Object LOCAL_RESERVATION_LOCK = new Object();

    private final CacheService cacheService;
    private final String redissonKeyHeader;
    private final ThreadLocal<String> currentKey = new ThreadLocal<>();

    public NoRepeatSubmitCheckerImpl(CacheService cacheService) {
        this(cacheService, null);
    }

    NoRepeatSubmitCheckerImpl(CacheService cacheService, String redissonKeyHeader) {
        this.cacheService = cacheService;
        this.redissonKeyHeader = redissonKeyHeader;
    }

    @Init
    public void register() {
        ValidatorManager.setNoRepeatSubmitChecker(this);
    }

    @Override
    public boolean check(NoRepeatSubmit anno, Context ctx, String submitHash, int limitSeconds) {
        currentKey.remove();

        // 唯一值（没有消息头则使用请求地址）
        String submitKey = StrUtil.trimToEmpty(ctx.header(SaManager.getConfig().getTokenName()));

        submitKey = SecureUtil.md5(submitKey + ":" + submitHash + ":" + requestBody(ctx));

        // 唯一标识（指定key + url + 消息头）
        String cacheRepeatKey = GlobalConstants.REPEAT_SUBMIT_KEY + ctx.method() + ":" + ctx.path() + submitKey;
        if (tryReserve(cacheRepeatKey, effectiveSeconds(anno, limitSeconds))) {
            currentKey.set(cacheRepeatKey);
            return true;
        }
        return false;
    }

    /** 原子占用防重复键，供两套兼容注解共用。 */
    public boolean tryReserve(String key, int seconds) {
        return tryReserve(key, Duration.ofSeconds(seconds));
    }

    /** 原子占用防重复键，保留自定义注解的毫秒精度。 */
    public boolean tryReserve(String key, Duration duration) {
        if (cacheService instanceof RedissonCacheService redisson) {
            return redisson.client().getBucket(redissonCacheKey(key))
                    .setIfAbsent("", duration);
        }

        synchronized (LOCAL_RESERVATION_LOCK) {
            return reserveWithCache(key, duration);
        }
    }

    /** 根据业务结果保留或释放当前请求写入的防重复键。 */
    public void complete(boolean success) {
        String key = currentKey.get();
        try {
            if (!success && StrUtil.isNotBlank(key)) {
                cacheService.remove(key);
            }
        } finally {
            currentKey.remove();
        }
    }

    public void release(String key) {
        if (StrUtil.isNotBlank(key)) {
            cacheService.remove(key);
        }
    }

    static int effectiveSeconds(NoRepeatSubmit anno, int limitSeconds) {
        return anno.seconds() == 1 && StrUtil.isBlank(anno.message()) ? BELL_DEFAULT_SECONDS : limitSeconds;
    }

    static String redissonCacheKey(String key, String keyHeader) {
        String digest = Utils.md5(key);
        return StrUtil.isBlank(keyHeader) ? digest : keyHeader + ":" + digest;
    }

    private String redissonCacheKey(String key) {
        String keyHeader = redissonKeyHeader;
        if (keyHeader == null) {
            keyHeader = Solon.cfg().get("jimuqu.cache.keyHeader", Solon.cfg().appName());
        }
        return redissonCacheKey(key, keyHeader);
    }

    private boolean reserveWithCache(String key, Duration duration) {
        if (ObjUtil.isNotNull(cacheService.get(key, String.class))) {
            return false;
        }
        long millis = duration.toMillis();
        int seconds = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (millis + 999L) / 1000L));
        cacheService.store(key, "", seconds);
        return true;
    }

    private String requestBody(Context ctx) {
        try {
            return StrUtil.trimToEmpty(ctx.bodyNew());
        } catch (Exception e) {
            log.warn("读取请求体失败，防重复提交校验退回框架摘要: {}", e.getMessage());
            return "";
        }
    }

}
