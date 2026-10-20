package com.jimuqu.common.core.utils;

import org.apache.fory.json.ForyJsonException;

/** 区分输入 JSON 错误与服务端响应编码失败。 */
public final class JsonReadException extends ForyJsonException {
    public JsonReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
