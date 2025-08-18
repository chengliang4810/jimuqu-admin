package com.jimuqu.common.core.constant;

/**
 * 租户常量信息
 *
 * @author Lion Li,chengliang4810
 */
public interface TenantConstants {

    /**
     * 租户正常状态
     */
    String NORMAL = "0";

    /**
     * 租户封禁状态
     */
    String DISABLE = "1";

    /**
     * 超级管理员ID
     */
    Long SUPER_ADMIN_ID = 1L;

    /**
     * 超级管理员角色 roleKey
     */
    String SUPER_ADMIN_ROLE_KEY = "superadmin";

    /**
     * 租户管理员角色 roleKey
     */
    String TENANT_ADMIN_ROLE_KEY = "admin";
}
