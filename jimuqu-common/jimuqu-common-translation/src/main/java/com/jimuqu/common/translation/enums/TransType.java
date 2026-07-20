package com.jimuqu.common.translation.enums;

/**
 * 翻译类型枚举
 */
public enum TransType {

    /**
     * 数据库字典翻译
     */
    DICT("dictTranslator"),
    /**
     * 枚举翻译
     */
    ENUM("enumTranslator"),
    /**
     * 默认翻译
     */
    DEFAULT("defaultTranslator"),
    /**
     * 用户 ID 转账号
     */
    USER_NAME("userNameTranslator"),
    /**
     * 用户 ID 转昵称
     */
    NICKNAME("nicknameTranslator"),
    /**
     * 部门 ID 转部门名称
     */
    DEPT_NAME("deptNameTranslator"),
    /**
     * OSS ID 转访问地址
     */
    OSS_URL("ossUrlTranslator");

    private final String translatorName;

    TransType(String translatorName) {
        this.translatorName = translatorName;
    }

    public String getTranslatorName() {
        return translatorName;
    }

}
