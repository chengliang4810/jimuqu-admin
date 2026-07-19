package com.jimuqu.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.enums.UserType;
import com.jimuqu.system.domain.SysUserRole;
import com.jimuqu.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;

/**
 * 事务提交后异步清理角色或用户的在线会话。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUserSessionCleaner {

    private final SysUserRoleMapper userRoleMapper;
    private final AfterCommitTaskExecutor afterCommitTaskExecutor;

    public void cleanRoleAfterCommit(Long roleId) {
        if (roleId == null) {
            return;
        }
        afterCommitTaskExecutor.execute(() -> cleanUsers(QueryChain.of(userRoleMapper)
                .select(SysUserRole::getUserId)
                .eq(SysUserRole::getRoleId, roleId)
                .returnType(Long.class)
                .list()));
    }

    public void cleanUsersAfterCommit(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<Long> snapshot = userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (!snapshot.isEmpty()) {
            afterCommitTaskExecutor.execute(() -> cleanUsers(snapshot));
        }
    }

    private void cleanUsers(Collection<Long> userIds) {
        for (Long userId : userIds) {
            for (UserType userType : UserType.values()) {
                try {
                    StpUtil.logout(userType.getUserType() + ":" + userId);
                } catch (RuntimeException ex) {
                    log.warn("清理用户在线会话失败，userId={}, type={}", userId, userType.getUserType(), ex);
                }
            }
        }
    }
}
