package com.jimuqu.common.core.sensitive.enums;

/**
 * 脱敏类型。
 *
 * @author chengliang
 */
public enum SensitiveType {

    /**
     * 手机号。
     */
    MOBILE,

    /**
     * 邮箱。
     */
    EMAIL,

    /**
     * 身份证号。
     */
    ID_CARD,

    /**
     * 银行卡号。
     */
    BANK_CARD,

    /**
     * 姓名。
     */
    NAME,

    /**
     * 地址。
     */
    ADDRESS,

    /**
     * 自定义前后保留位。
     */
    CUSTOM
}
