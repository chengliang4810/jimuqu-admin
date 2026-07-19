package com.jimuqu.common.web.validation;

import com.jimuqu.common.core.utils.MessageUtils;
import org.noear.solon.validation.BeanValidateInfo;
import org.noear.solon.validation.ValidatorException;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NoRepeatSubmit;

import java.lang.annotation.Annotation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将校验注解中的消息键解析为面向用户的国际化消息。
 */
public final class ValidationMessageResolver {

    private static final Pattern MESSAGE_KEY = Pattern.compile("^\\{([\\w.]+)}$");

    private ValidationMessageResolver() {
    }

    public static String resolve(ValidatorException exception, String acceptLanguage) {
        return resolve(exception, null, acceptLanguage);
    }

    /**
     * 按 Bell/Jimu 契约优先使用 Content-Language，缺失时兼容 Accept-Language。
     */
    public static String resolve(ValidatorException exception, String contentLanguage, String acceptLanguage) {
        String message = exception.getMessage();
        Annotation annotation = validationAnnotation(exception);
        if (annotation instanceof NoRepeatSubmit
                && (message == null || message.isBlank() || message.startsWith("@NoRepeatSubmit"))) {
            message = "{repeat.submit.message}";
        }
        Matcher matcher = MESSAGE_KEY.matcher(message == null ? "" : message);
        if (!matcher.matches()) {
            return message;
        }

        String resolved = MessageUtils.message(matcher.group(1),
                MessageUtils.resolveLocale(contentLanguage, acceptLanguage));
        if (resolved.equals(matcher.group(1))) {
            return message;
        }
        if (annotation instanceof Length length) {
            return resolved.replace("{min}", String.valueOf(length.min()))
                    .replace("{max}", String.valueOf(length.max()));
        }
        return resolved;
    }

    /** 上游 Bean、方法参数和重复提交校验均使用默认失败码 500。 */
    public static int errorCode(ValidatorException exception) {
        return 500;
    }

    private static Annotation validationAnnotation(ValidatorException exception) {
        if (exception.getAnnotation() != null) {
            return exception.getAnnotation();
        }
        if (exception.getResult() != null && exception.getResult().getData() instanceof BeanValidateInfo info) {
            return info.anno;
        }
        return null;
    }

}
