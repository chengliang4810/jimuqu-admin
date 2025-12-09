package com.jimuqu.common.core.utils;

import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.map.Dict;
import cn.hutool.v7.core.text.StrPool;
import cn.hutool.v7.core.util.ObjUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.noear.snack4.Feature;
import org.noear.snack4.ONode;
import org.noear.snack4.codec.TypeRef;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * JSON 工具类
 * 需要什么就 to一下
 * @author chengliang
 * @date 2024/08/07
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtil {

    /**
     * 对象转 JsonString
     *
     * @param object 实体对象
     * @return JsonString
     */
    public static String toString(Object object) {
        if (ObjUtil.isNull(object)) {
            return null;
        }
        return ONode.serialize(object);
    }

    /**
     * JsonString美化格式化
     *
     * @param object 实体对象/未格式化的json字符串
     * @return {@link String } 格式化后的json字符串
     */
    public static String toStringFormat(Object object) {
        if (ObjUtil.isNull(object)) {
            return StrPool.EMPTY_JSON;
        }
        return ONode.serialize(object, Feature.Write_PrettyFormat);
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
        return ONode.deserialize(jsonString, type);
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
            return ListUtil.zero();
        }
        return ONode.deserialize(jsonString, new TypeRef<List<T>>(){}.getType());
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
            return SetUtil.zero();
        }
        return ONode.deserialize(jsonString, new TypeRef<Set<T>>(){}.getType());
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
        return ONode.deserialize(jsonString, Dict.class);
    }

}
