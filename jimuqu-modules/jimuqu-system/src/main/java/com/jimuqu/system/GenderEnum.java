package com.jimuqu.system;

import com.jimuqu.common.translation.core.TranslatableEnum;
import lombok.Getter;

@Getter
public enum GenderEnum implements TranslatableEnum<String> {

    MAN("0", "男"),
    WOMAN("1", "女"),
    UNKNOWN("2", "未知"),
    ;

    private final String value;
    private final String label;

    GenderEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public String getValue() {
        return value;
    }
    @Override
    public String getLabel() {
        return label;
    }

}
