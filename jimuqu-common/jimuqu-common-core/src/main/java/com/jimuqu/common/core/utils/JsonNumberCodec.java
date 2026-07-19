package com.jimuqu.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.noear.snack4.Options;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * JSON 数字编码规则，避免浏览器解析大整数时丢失精度。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonNumberCodec {

    private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;
    private static final long MIN_SAFE_INTEGER = -MAX_SAFE_INTEGER;
    private static final BigInteger MAX_SAFE_BIG_INTEGER = BigInteger.valueOf(MAX_SAFE_INTEGER);
    private static final BigInteger MIN_SAFE_BIG_INTEGER = BigInteger.valueOf(MIN_SAFE_INTEGER);

    /**
     * 为 Snack4 写配置注册与上游一致的浏览器安全数字编码规则。
     *
     * @param options Snack4 写配置
     */
    public static void configure(Options options) {
        options.addEncoder(Long.class, (ctx, value, target) -> isSafe(value)
                ? target.setValue(value)
                : target.setValue(value.toString()));
        options.addEncoder(BigInteger.class, (ctx, value, target) -> isSafe(value)
                ? target.setValue(value)
                : target.setValue(value.toString()));
        options.addEncoder(BigDecimal.class, (ctx, value, target) -> target.setValue(value.toString()));
    }

    private static boolean isSafe(long value) {
        return value >= MIN_SAFE_INTEGER && value <= MAX_SAFE_INTEGER;
    }

    private static boolean isSafe(BigInteger value) {
        return value.compareTo(MIN_SAFE_BIG_INTEGER) >= 0 && value.compareTo(MAX_SAFE_BIG_INTEGER) <= 0;
    }
}
