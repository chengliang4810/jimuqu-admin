package com.jimuqu.common.web.config;

import org.junit.jupiter.api.Test;
import com.jimuqu.common.web.serialization.ForyJsonStringSerializer;
import com.jimuqu.common.web.sensitive.SensitiveJsonRender;
import org.noear.solon.serialization.prop.JsonProps;
import org.noear.solon.core.handle.ContextEmpty;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebConfigJsonTest {
    @Test
    void appliesConfiguredDateFormatAndTimezoneToBothResponsePaths() throws Throwable {
        JsonProps props = new JsonProps();
        props.dateAsFormat = "yyyy-MM-dd HH:mm:ss";
        props.dateAsTimeZone = "GMT+8";
        ForyJsonStringSerializer serializer = new ForyJsonStringSerializer(props);
        Object value = List.of(new Date(0), Instant.EPOCH,
                LocalDateTime.of(1970, 1, 1, 8, 0), LocalDate.of(1970, 1, 1));
        String expected = "[\"1970-01-01 08:00:00\",\"1970-01-01 08:00:00\","
                + "\"1970-01-01 08:00:00\",\"1970-01-01 00:00:00\"]";
        assertEquals(expected, serializer.serialize(value));
        assertEquals(expected, new SensitiveJsonRender(serializer).renderAndReturn(value, ContextEmpty.create()));
        props.dateAsTicks = true;
        assertEquals("[0]", new ForyJsonStringSerializer(props).serialize(List.of(new Date(0))));
        assertEquals("0", com.jimuqu.common.core.utils.JsonUtil.toString(new Date(0)));
    }


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
