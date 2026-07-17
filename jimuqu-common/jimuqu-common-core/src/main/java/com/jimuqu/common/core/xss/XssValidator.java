package com.jimuqu.common.core.xss;

import cn.hutool.v7.core.regex.ReUtil;
import com.jimuqu.common.core.utils.StringUtil;
import org.noear.solon.Utils;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;
import org.noear.solon.validation.Validator;

/**
 * 自定义xss校验注解实现
 *
 * @author Lion Li,chengliang4810
 */
public class XssValidator implements Validator<Xss> {

    public static final XssValidator INSTANCE = new XssValidator();

    @Override
    public String message(Xss annotation) {
        return annotation.message();
    }

    @Override
    public Class<?>[] groups(Xss annotation) {
        return annotation.groups();
    }

    @Override
    public boolean isSupportValueType(Class<?> type) {
        return String.class.isAssignableFrom(type);
    }

    @Override
    public Result validateOfValue(Xss annotation, Object value, StringBuilder tmp) {
        return isValid((String) value) ? Result.succeed() : Result.failure();
    }

    @Override
    public Result validateOfContext(Context ctx, Xss annotation, String name, StringBuilder tmp) {
        return isValid(ctx.param(name)) ? Result.succeed() : Result.failure(name);
    }

    private boolean isValid(String value) {
        return Utils.isEmpty(value) || !ReUtil.contains(StringUtil.RE_HTML_MARK, value);
    }

}
