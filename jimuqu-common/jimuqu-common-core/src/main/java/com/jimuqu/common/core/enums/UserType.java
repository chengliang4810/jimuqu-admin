package com.jimuqu.common.core.enums;

import com.jimuqu.common.core.utils.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备类型
 * 针对多套 用户体系
 *
 * @author Lion Li,chengliang4810
 */
@Getter
@AllArgsConstructor
public enum UserType {

    /**
     * 系统用户
     */
    SYS_USER("sys_user"),

    /**
     * app端
     */
    APP_USER("app_user" );

    private final String userType;

    public static UserType getUserType(String str) {
        for (UserType value : values()) {
            if (StringUtil.contains(str, value.getUserType())) {
                return value;
            }
        }
        throw new RuntimeException("'UserType' not found By " + str);
    }
}
