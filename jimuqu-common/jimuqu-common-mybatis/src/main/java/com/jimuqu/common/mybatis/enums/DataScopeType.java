package com.jimuqu.common.mybatis.enums;

import com.jimuqu.common.core.utils.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限类型。
 */
@Getter
@AllArgsConstructor
public enum DataScopeType {

    ALL("1"),
    CUSTOM("2"),
    DEPT("3"),
    DEPT_AND_CHILD("4"),
    SELF("5");

    private final String code;

    public static DataScopeType findCode(String code) {
        if (StringUtil.isBlank(code)) {
            return null;
        }
        for (DataScopeType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
