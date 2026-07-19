package com.jimuqu.common.core.service;

/**
 * 响应字段脱敏权限契约。
 */
@FunctionalInterface
public interface SensitiveService {

    /**
     * 判断当前访问者是否需要脱敏。
     *
     * @param roleKey 允许查看原文的角色标识
     * @param perms 允许查看原文的权限标识
     * @return 需要脱敏返回 {@code true}
     */
    boolean isSensitive(String[] roleKey, String[] perms);
}
