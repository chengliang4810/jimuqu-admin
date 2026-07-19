package com.jimuqu.common.core.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JsonUtilNumberTest {

    @Test
    void writesOnlyUnsafeIntegersAsStrings() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("small", 10L);
        value.put("maxSafe", 9_007_199_254_740_991L);
        value.put("unsafe", 9_007_199_254_740_992L);
        value.put("negativeUnsafe", -9_007_199_254_740_992L);
        value.put("bigInteger", new BigInteger("9007199254740993"));
        value.put("decimal", new BigDecimal("12.3400"));

        assertEquals(
                "{\"small\":10,\"maxSafe\":9007199254740991,\"unsafe\":\"9007199254740992\"," +
                        "\"negativeUnsafe\":\"-9007199254740992\",\"bigInteger\":\"9007199254740993\"," +
                        "\"decimal\":\"12.3400\"}",
                JsonUtil.toString(value));
    }

    @Test
    void honorsCollectionElementTypesDuringDeserialization() {
        String json = "[{\"name\":\"admin\"}]";

        List<UserView> list = JsonUtil.toObjectList(json, UserView.class);
        Set<UserView> set = JsonUtil.toObjectSet(json, UserView.class);

        assertInstanceOf(UserView.class, list.get(0));
        assertEquals("admin", list.get(0).name);
        assertInstanceOf(UserView.class, set.iterator().next());
    }

    public static class UserView {
        public String name;
    }
}
