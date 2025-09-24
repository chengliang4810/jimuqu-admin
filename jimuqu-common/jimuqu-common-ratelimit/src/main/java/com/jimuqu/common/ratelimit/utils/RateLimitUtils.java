package com.jimuqu.common.ratelimit.utils;

import cn.hutool.v7.core.text.StrUtil;
import com.jimuqu.common.ratelimit.enums.RateLimitType;
import com.jimuqu.common.satoken.utils.LoginHelper;
import org.noear.solon.core.handle.Context;

import java.lang.reflect.Method;

/**
 * 限流工具类
 */
public class RateLimitUtils {

    /**
     * 构建限流键
     * @param method 方法
     * @param key 自定义键
     * @param ip IP地址
     * @param userId 用户ID
     * @param type 限流类型
     * @return 限流键
     */
    public static String buildRateLimitKey(Method method, String key, String ip, Long userId,
                                         RateLimitType type) {
        StringBuilder keyBuilder = new StringBuilder();

        switch (type) {
            case IP:
                keyBuilder.append("ip:");
                if (StrUtil.isNotEmpty(ip)) {
                    keyBuilder.append(ip);
                } else {
                    // 如果获取不到IP，使用unknown
                    keyBuilder.append("unknown");
                }
                break;

            case USER:
                keyBuilder.append("user:");
                if (userId != null) {
                    keyBuilder.append(userId);
                } else {
                    // 如果获取不到用户ID，使用anonymous
                    keyBuilder.append("anonymous");
                }
                break;

            case GLOBAL:
            default:
                if (StrUtil.isNotEmpty(key)) {
                    keyBuilder.append(key);
                } else {
                    keyBuilder.append(method.getDeclaringClass().getSimpleName())
                             .append(":")
                             .append(method.getName());
                }
                break;
        }

        return keyBuilder.toString();
    }

    /**
     * 获取客户端IP地址
     * @param context HTTP上下文
     * @return IP地址
     */
    public static String getClientIp(Context context) {
        if (context == null) {
            return "";
        }

        return context.current().realIp();
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        try {
            return LoginHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}