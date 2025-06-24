package com.jimuqu.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.hutool.core.map.Dict;
import org.dromara.hutool.core.util.ObjUtil;
import org.noear.snack.ONode;

import java.lang.reflect.Type;

/**
 * JSON 工具类
 *
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
    public static String toJsonString(Object object) {
        if (ObjUtil.isNull(object)) {
            return null;
        }
        return ONode.stringify(object);
    }

    /**
     * Json字符串转对象
     *
     * @param jsonString json 字符串
     * @param type      类型
     * @return 实体类型
     */
    public static <T> T parseObject(String jsonString, Type type) {
        if (StringUtil.isEmpty(jsonString)) {
            return null;
        }
        return ONode.deserialize(jsonString, type);
    }

    /**
     * json 转 map
     *
     * @param jsonString json 字符串
     * @return map 对象
     */
    public static Dict parseMap(String jsonString) {
        if (StringUtil.isBlank(jsonString)) {
            return Dict.of();
        }
        return ONode.deserialize(jsonString, Dict.class);
    }

}
