package com.jimuqu.common.web.config;

import org.junit.jupiter.api.Test;
import com.jimuqu.common.web.serialization.ForyJsonStringSerializer;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebConfigJsonTest {

    @Test
    void configuresBrowserSafeNumberEncodingInsteadOfAllLongsAsStrings() throws Exception {
        ForyJsonStringSerializer serializer = new ForyJsonStringSerializer();

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("timeout", 1800L);
        value.put("id", 350_000_000_000_000_001L);
        value.put("amount", new BigDecimal("1.20"));
        value.put("clientKey", null);
        assertEquals("{\"timeout\":1800,\"id\":\"350000000000000001\",\"amount\":\"1.20\",\"clientKey\":null}",
                serializer.serialize(value));
    }
}
