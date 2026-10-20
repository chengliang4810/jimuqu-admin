package com.jimuqu.common.cache;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonForyCodec;
import org.redisson.codec.TypedJsonForyCodec;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 仅验证发布包的 codec，应用现有 Redis 编码格式不在此切换。 */
class RedissonForyJsonCodecTest {
    @Test
    void untypedForyJsonRestoresTheObjectTypeAndExactNumbers() throws Exception {
        verify(new JsonForyCodec(), true);
    }

    @Test
    void typedForyJsonUsesAnExplicitTypeWithoutClassMetadata() throws Exception {
        verify(new TypedJsonForyCodec(CacheRecord.class), false);
    }

    private void verify(Codec codec, boolean metadata) throws Exception {
        CacheRecord value = new CacheRecord();
        value.id = 9_007_199_254_740_992L;
        value.amount = new BigDecimal("12345678901234567890.123400");
        value.tags = new ArrayList<>(List.of("中文", "fory"));
        ByteBuf buffer = codec.getValueEncoder().encode(value);
        try {
            String text = buffer.toString(StandardCharsets.UTF_8);
            assertTrue(text.startsWith("{"), text);
            assertEquals(metadata, text.contains("\"@class\""), text);
            CacheRecord restored = assertInstanceOf(CacheRecord.class,
                    codec.getValueDecoder().decode(buffer, null));
            assertEquals(value.id, restored.id);
            assertEquals(value.amount, restored.amount);
            assertEquals(value.tags, restored.tags);
        } finally {
            buffer.release();
        }
    }

    public static class CacheRecord {
        public Long id;
        public BigDecimal amount;
        public List<String> tags;
    }
}
