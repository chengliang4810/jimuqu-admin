package com.jimuqu.common.core.utils;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ObjectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.fory.json.ForyJson;
import org.apache.fory.reflect.TypeRef;
import cn.hutool.core.lang.ParameterizedTypeImpl;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON 工具类
 * 需要什么就 to一下
 * @author chengliang
 * @date 2024/08/07
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtil {

    private static final ForyJson JSON = ForyJson.builder()
            .withFieldMode(true)
            .writeNullFields(true)
            .byteArrayFormat(org.apache.fory.json.annotation.JsonByteArray.Format.ARRAY)
            .maxDepth(64)
            .registerCodec(java.util.Date.class, new JsonDateCodec())
            .registerCodec(Object.class, new JsonObjectCodec())
            .build();

    /**
     * 对象转 JsonString
     *
     * @param object 实体对象
     * @return JsonString
     */
    public static String toString(Object object) {
        if (ObjectUtil.isNull(object)) {
            return null;
        }
        return JSON.toJson(JsonWriteSupport.toTree(object, false, null));
    }

    /**
     * JsonString美化格式化
     *
     * @param object 实体对象/未格式化的json字符串
     * @return {@link String } 格式化后的json字符串
     */
    public static String toStringFormat(Object object) {
        if (ObjectUtil.isNull(object)) {
            return StrPool.EMPTY_JSON;
        }
        return JSON.toPrettyJson(JsonWriteSupport.toTree(object, false, null));
    }

    /**
     * Json字符串转对象
     *
     * @param jsonString json 字符串
     * @param type      类型
     * @return 实体类型
     */
    public static <T> T toObject(String jsonString, Type type) {
        if (StringUtil.isEmpty(jsonString)) {
            return null;
        }
        if (type instanceof cn.hutool.core.lang.TypeReference<?> reference) {
            type = reference.getType();
        }
        try {
            return JSON.fromJson(jsonString, TypeRef.of(type == null ? Object.class : type));
        } catch (org.apache.fory.json.ForyJsonException e) {
            throw new JsonReadException("Invalid JSON input", e);
        }
    }

    /**
     * Json字符串转对象列表
     *
     * @param jsonString json 字符串
     * @param type       元素类型
     * @return 实体类型列表
     */
    public static <T> List<T> toObjectList(String jsonString, Class<T> type) {
        if (StringUtil.isEmpty(jsonString)) {
            return java.util.Collections.emptyList();
        }
        return toObject(jsonString, new ParameterizedTypeImpl(new Type[]{type}, null, List.class));
    }

    /**
     * Json字符串转对象Set列表
     *
     * @param jsonString json 字符串
     * @param type       元素类型
     * @return 实体类型列表
     */
    public static <T> Set<T> toObjectSet(String jsonString, Class<T> type) {
        if (StringUtil.isEmpty(jsonString)) {
            return java.util.Collections.emptySet();
        }
        return toObject(jsonString, new ParameterizedTypeImpl(new Type[]{type}, null, Set.class));
    }

    /**
     * json 转 map
     *
     * @param jsonString json 字符串
     * @return map 对象
     */
    public static Dict toMap(String jsonString) {
        if (StringUtil.isBlank(jsonString)) {
            return Dict.of();
        }
        Map<String, Object> values = toObject(jsonString, Map.class);
        if (values == null) {
            return null;
        }
        Dict result = Dict.of();
        result.putAll(values);
        return result;
    }

    /** 编码已经投影过的 JSON 值；保留 null 和现有数值类型。 */
    public static String writeTree(Object value) {
        return JSON.toJson(value);
    }

}
