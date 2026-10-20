package com.jimuqu.common.core.utils;

import org.apache.fory.json.codec.JsonValueCodec;
import org.apache.fory.json.reader.JsonReader;
import org.apache.fory.json.reader.Latin1JsonReader;
import org.apache.fory.json.reader.Utf16JsonReader;
import org.apache.fory.json.reader.Utf8JsonReader;
import org.apache.fory.json.writer.StringJsonWriter;
import org.apache.fory.json.writer.Utf8JsonWriter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** 无类型 JSON 值保留小整数类型和十进制精度，避免中间 Map 绑定丢失金额精度。 */
final class JsonObjectCodec implements JsonValueCodec<Object> {
    @Override
    public void writeString(StringJsonWriter writer, Object value) {
        if (value == null) {
            writer.writeNull();
        } else if (value.getClass() == Object.class) {
            writer.writeObjectStart();
            writer.writeObjectEnd();
        } else {
            writer.typeResolver().getRuntimeTypeInfo(value.getClass()).stringWriter().writeString(writer, value);
        }
    }

    @Override
    public void writeUtf8(Utf8JsonWriter writer, Object value) {
        if (value == null) {
            writer.writeNull();
        } else if (value.getClass() == Object.class) {
            writer.writeObjectStart();
            writer.writeObjectEnd();
        } else {
            writer.typeResolver().getRuntimeTypeInfo(value.getClass()).utf8Writer().writeUtf8(writer, value);
        }
    }

    @Override
    public Object readLatin1(Latin1JsonReader reader) {
        return read(reader);
    }

    @Override
    public Object readUtf16(Utf16JsonReader reader) {
        return read(reader);
    }

    @Override
    public Object readUtf8(Utf8JsonReader reader) {
        return read(reader);
    }

    private Object read(JsonReader reader) {
        char token = reader.peekToken();
        if (token == '"') {
            return reader.readString();
        }
        if (token == 'n') {
            reader.readNull();
            return null;
        }
        if (token == 't' || token == 'f') {
            return reader.readBoolean();
        }
        if (token == '{' || token == '[') {
            reader.enterDepth();
            reader.reserveGraphMemory(64);
            try {
                if (token == '{') {
                    var result = new LinkedHashMap<String, Object>();
                    reader.expectNextToken('{');
                    if (!reader.consumeNextToken('}')) {
                        do {
                            String key = reader.readFieldName();
                            reader.expectNextToken(':');
                            reader.reserveGraphMemory(48);
                            result.put(key, read(reader));
                        } while (reader.consumeNextToken(','));
                        reader.expectNextToken('}');
                    }
                    return result;
                }
                var result = new ArrayList<>();
                reader.expectNextToken('[');
                if (!reader.consumeNextToken(']')) {
                    do {
                        reader.reserveGraphMemory(16);
                        result.add(read(reader));
                    } while (reader.consumeNextToken(','));
                    reader.expectNextToken(']');
                }
                return result;
            } finally {
                reader.exitDepth();
            }
        }
        BigDecimal number = reader.readBigDecimal();
        if (number.scale() != 0) {
            return number;
        }
        BigInteger integer = number.toBigIntegerExact();
        if (integer.bitLength() < 32) {
            return integer.intValue();
        }
        if (integer.bitLength() < 64) {
            return integer.longValue();
        }
        return integer;
    }
}
