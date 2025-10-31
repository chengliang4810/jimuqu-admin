package com.jimuqu.system;

import com.jimuqu.common.translation.core.TranslatableEnum;
import lombok.Getter;

@Getter
public enum GenderEnum implements TranslatableEnum<String> {

    UNKNOWN("0", "未知"),
    MAN("1", "男"),
    WOMAN("2", "女"),
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
