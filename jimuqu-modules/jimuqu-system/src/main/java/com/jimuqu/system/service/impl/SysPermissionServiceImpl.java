package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.constant.GlobalConstants;
import com.jimuqu.system.service.ISysPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Set;

@Slf4j
@Component
public class SysPermissionServiceImpl implements ISysPermissionService {

    @Override
    public Set<String> getRolePermission(Long userId) {
        return Set.of(GlobalConstants.SUPER_ADMIN_ROLE_KEY);
    }

    @Override
    public Set<String> getMenuPermission(Long userId) {
        return Set.of("*:*:*");
    }
}
