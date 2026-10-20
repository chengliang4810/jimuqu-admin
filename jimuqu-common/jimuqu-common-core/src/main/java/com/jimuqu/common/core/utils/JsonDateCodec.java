package com.jimuqu.common.core.utils;

import org.apache.fory.json.ForyJsonException;
import org.apache.fory.json.codec.AbstractJsonValueCodec;
import org.apache.fory.json.reader.JsonReader;
import org.apache.fory.json.writer.JsonWriter;

import java.util.Date;

/** 兼容已有时间戳与日期字符串输入，日期输出沿用毫秒时间戳。 */
final class JsonDateCodec extends AbstractJsonValueCodec<Date> {
    @Override
    public void write(JsonWriter writer, Date value) {
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeLong(value.getTime());
        }
    }

    @Override
    public Date read(JsonReader reader) {
        if (reader.tryReadNullToken()) {
            return null;
        }
        if (reader.peekToken() != '"') {
            return new Date(reader.readLong());
        }
        String value = reader.readString();
        try {
            if (value.matches("-?\\d+")) {
                return new Date(Long.parseLong(value));
            }
            return cn.hutool.core.date.DateUtil.parse(value);
        } catch (RuntimeException e) {
            throw new ForyJsonException("Invalid JSON date", e);
        }
    }
}
