package com.jimuqu.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
     * 保留安全整数的数字类型，超出浏览器精度范围的整数和金额使用字符串。
     */
    public static Object encode(Number value) {
        if (value instanceof Long longValue && !isSafe(longValue)
                || value instanceof BigInteger bigValue && !isSafe(bigValue)
                || value instanceof BigDecimal) {
            return value.toString();
        }
        return value;
    }

    private static boolean isSafe(long value) {
        return value >= MIN_SAFE_INTEGER && value <= MAX_SAFE_INTEGER;
    }

    private static boolean isSafe(BigInteger value) {
        return value.compareTo(MIN_SAFE_BIG_INTEGER) >= 0 && value.compareTo(MAX_SAFE_BIG_INTEGER) <= 0;
    }
}
