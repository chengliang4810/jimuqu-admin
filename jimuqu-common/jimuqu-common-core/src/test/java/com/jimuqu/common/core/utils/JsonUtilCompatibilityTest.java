package com.jimuqu.common.core.utils;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.TypeReference;
import org.apache.fory.json.ForyJsonException;
import org.apache.fory.json.annotation.JsonIgnore;
import org.apache.fory.json.annotation.JsonInclude;
import org.apache.fory.json.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilCompatibilityTest {
    @Test
    void acceptsHutoolTypeReferenceDirectlyForFileMetadataAndNestedValues() {
        Map<String, String> metadata = JsonUtil.toObject("{\"content-type\":\"text/plain\"}",
                new TypeReference<Map<String, String>>() {});
        assertEquals(Map.of("content-type", "text/plain"), metadata);
        Map<String, List<Long>> nested = JsonUtil.toObject("{\"ids\":[1,350000000000000001]}",
                new TypeReference<Map<String, List<Long>>>() {});
        assertEquals(List.of(1L, 350_000_000_000_000_001L), nested.get("ids"));
    }

    @Test
    void preservesTinyNonzeroDecimalsWhenOmittingDefaultValues() {
        DefaultValues value = new DefaultValues();
        value.amount = new BigDecimal("1e-1000");
        Dict tree = JsonUtil.toMap(JsonUtil.toString(value));
        assertEquals(value.amount.toString(), tree.get("amount"));
        assertFalse(tree.containsKey("zero"));
        assertFalse(tree.containsKey("disabled"));
    }

    @JsonInclude(JsonProperty.Include.NON_DEFAULT)
    public static class DefaultValues {
        public BigDecimal amount;
        public int zero;
        public boolean disabled;
    }

    @Test
    void preservesNullEmptyAndPrettyPrintingContracts() {
        assertNull(JsonUtil.toString(null));
        assertEquals("{}", JsonUtil.toStringFormat(null));
        assertNull(JsonUtil.toObject("", Object.class));
        assertEquals(List.of(), JsonUtil.toObjectList("", View.class));
        assertTrue(JsonUtil.toObjectSet("", View.class).isEmpty());
        assertTrue(JsonUtil.toMap(" ").isEmpty());
        var value = new LinkedHashMap<String, Object>();
        value.put("present", 1);
        value.put("missing", null);
        assertEquals("{\"present\":1}", JsonUtil.toString(value));
        assertTrue(JsonUtil.toStringFormat(value).contains("\n"));
    }

    @Test
    void bindsGenericContainersAliasesIgnoredFieldsAndDates() {
        String text = """
                {"items":[{"userId":"350000000000000001","createdAt":"2026-10-20 12:30:00",
                           "password":"must-not-bind","amount":"12.3400"}]}
                """;
        Map<String, List<View>> value = JsonUtil.toObject(text,
                new TypeReference<Map<String, List<View>>>() {}.getType());
        View item = value.get("items").getFirst();
        assertEquals(350_000_000_000_000_001L, item.id);
        assertNotNull(item.createdAt);
        assertNull(item.password);
        assertEquals(new BigDecimal("12.3400"), item.amount);
        Dict dict = JsonUtil.toObject("{\"key\":1}", Dict.class);
        assertEquals(1, dict.get("key"));
        assertTrue(JsonUtil.toString(item).contains("\"userId\":\"350000000000000001\""));
        assertFalse(JsonUtil.toString(item).contains("password"));
    }

    @Test
    void preservesUntypedIntegerRangesAndDecimalPrecision() {
        Map<?, ?> value = JsonUtil.toObject("""
                {"small":1,"min":-2147483648,"large":3000000000,
                 "big":9223372036854775808,"amount":12345678901234567890.123400}
                """, Map.class);
        assertInstanceOf(Integer.class, value.get("small"));
        assertEquals(Integer.MIN_VALUE, value.get("min"));
        assertEquals(3_000_000_000L, value.get("large"));
        assertEquals(new BigInteger("9223372036854775808"), value.get("big"));
        assertEquals(new BigDecimal("12345678901234567890.123400"), value.get("amount"));
    }

    @Test
    void keepsArrayDatesAndBoundaryNumbersCompatible() {
        assertEquals("[1,-2]", JsonUtil.toString(new byte[]{1, -2}));
        assertArrayEquals(new byte[]{1, -2}, JsonUtil.toObject("[1,-2]", byte[].class));
        assertEquals("0", JsonUtil.toString(new Date(0)));
        assertEquals(new Date(0), JsonUtil.toObject("0", Date.class));
        assertEquals("[-9007199254740991,\"-9007199254740992\"]",
                JsonUtil.toString(new long[]{-9_007_199_254_740_991L, -9_007_199_254_740_992L}));
    }

    @Test
    void rejectsMalformedInputAndCircularOutputWithoutMutatingObjects() {
        for (String input : List.of("{", "[1,]", "{\"key\":1} trailing", "{\"key\":01}")) {
            assertThrows(JsonReadException.class, () -> JsonUtil.toObject(input, Object.class));
        }
        View view = new View();
        view.id = 350_000_000_000_000_001L;
        JsonUtil.toString(view);
        assertEquals(350_000_000_000_000_001L, view.id);
        Map<String, Object> circular = new LinkedHashMap<>();
        circular.put("self", circular);
        assertThrows(ForyJsonException.class, () -> JsonUtil.toString(circular));
    }

    public static class View {
        @JsonProperty("userId")
        public Long id;
        @JsonIgnore
        public String password;
        public Date createdAt;
        public BigDecimal amount;
    }
}
