package com.jimuqu.common.web.config;

import org.junit.jupiter.api.Test;
import org.noear.snack4.Feature;
import org.noear.solon.serialization.snack4.Snack4StringSerializer;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebConfigJsonTest {

    @Test
    void configuresBrowserSafeNumberEncodingInsteadOfAllLongsAsStrings() throws Exception {
        Snack4StringSerializer serializer = new Snack4StringSerializer();
        serializer.getSerializeConfig().addFeatures(
                Feature.Write_LongAsString,
                Feature.Write_NullListAsEmpty,
                Feature.Write_NullStringAsEmpty);

        WebConfig.configureJsonSerializer(serializer);

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("timeout", 1800L);
        value.put("id", 350_000_000_000_000_001L);
        value.put("amount", new BigDecimal("1.20"));
        value.put("clientKey", null);
        assertEquals("{\"timeout\":1800,\"id\":\"350000000000000001\",\"amount\":\"1.20\",\"clientKey\":null}",
                serializer.serialize(value));
        assertFalse(serializer.getSerializeConfig().getOptions().hasFeature(Feature.Write_LongAsString));
        assertFalse(serializer.getSerializeConfig().getOptions().hasFeature(Feature.Write_NullListAsEmpty));
        assertFalse(serializer.getSerializeConfig().getOptions().hasFeature(Feature.Write_NullStringAsEmpty));
        assertTrue(serializer.getSerializeConfig().getOptions().hasFeature(Feature.Write_Nulls));
    }
}
