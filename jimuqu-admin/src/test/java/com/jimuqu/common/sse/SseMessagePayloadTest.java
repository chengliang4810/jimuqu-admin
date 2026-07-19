package com.jimuqu.common.sse;

import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.sse.domain.SseMessagePayload;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SseMessagePayloadTest {

    @Test
    void serializesTimestampAsJsonNumber() {
        Map<?, ?> payload = JsonUtil.toObject(
                JsonUtil.toString(SseMessagePayload.message("test")), Map.class);

        assertInstanceOf(Number.class, payload.get("timestamp"));
    }
}
