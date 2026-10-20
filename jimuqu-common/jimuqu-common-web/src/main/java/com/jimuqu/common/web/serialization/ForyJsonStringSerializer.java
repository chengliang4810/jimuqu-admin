package com.jimuqu.common.web.serialization;

import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.JsonWriteSupport;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ModelAndView;
import org.noear.solon.serialization.EntitySerializer;
import org.noear.solon.serialization.prop.JsonProps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.function.BiFunction;

/** Apache Fory JSON 的 Solon 文本序列化适配。 */
public final class ForyJsonStringSerializer implements EntitySerializer<String> {
    private final DateTimeFormatter dateFormat;
    private final ZoneId zone;

    public ForyJsonStringSerializer() {
        this(new JsonProps());
    }

    public ForyJsonStringSerializer(JsonProps props) {
        zone = props.dateAsTimeZone == null || props.dateAsTimeZone.isBlank()
                ? ZoneId.systemDefault() : ZoneId.of(props.dateAsTimeZone);
        dateFormat = props.dateAsTicks || props.dateAsFormat == null || props.dateAsFormat.isBlank()
                ? null : DateTimeFormatter.ofPattern(props.dateAsFormat).withZone(zone);
    }

    public Object toTree(Object value, BiFunction<Field, Object, Object> fieldTransform) {
        return JsonWriteSupport.toTree(value, true, fieldTransform, this::encodeDate);
    }

    private Object encodeDate(Object value) {
        if (value instanceof LocalDate date) {
            return encodeDate(date.atStartOfDay());
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateFormat == null ? dateTime.atZone(zone).toInstant().toEpochMilli()
                    : dateFormat.format(dateTime);
        }
        Instant instant;
        if (value instanceof Date date) {
            instant = Instant.ofEpochMilli(date.getTime());
        } else if (value instanceof Instant date) {
            instant = date;
        } else if (value instanceof OffsetDateTime date) {
            instant = date.toInstant();
        } else if (value instanceof ZonedDateTime date) {
            instant = date.toInstant();
        } else {
            return value.toString();
        }
        return dateFormat == null ? instant.toEpochMilli() : dateFormat.format(instant);
    }
    @Override
    public String name() {
        return "fory-json";
    }

    @Override
    public String mimeType() {
        return "application/json;charset=UTF-8";
    }

    @Override
    public Class<String> dataType() {
        return String.class;
    }

    @Override
    public boolean matched(Context ctx, String mime) {
        if (mime == null) {
            return false;
        }
        String mediaType = mime.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return "application/json".equals(mediaType) || "text/json".equals(mediaType)
                || mediaType.startsWith("application/") && mediaType.endsWith("+json");
    }

    @Override
    public String serialize(Object value) {
        return JsonUtil.writeTree(toTree(value, null));
    }

    @Override
    public Object deserialize(String data, Type type) {
        return JsonUtil.toObject(data, type);
    }

    @Override
    public void serializeToBody(Context ctx, Object value) throws IOException {
        if (ctx.contentTypeNew() == null) {
            ctx.contentType(mimeType());
        }
        ctx.output(serialize(value instanceof ModelAndView view ? view.model() : value));
    }

    @Override
    public Object deserializeFromBody(Context ctx, Type type) throws IOException {
        return deserialize(ctx.bodyNew(), type);
    }
}
