package com.jimuqu.common.web.validation;

import org.noear.solon.validation.BeanValidateInfo;
import org.noear.solon.validation.ValidatorException;
import org.noear.solon.validation.annotation.Length;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将校验注解中的消息键解析为面向用户的国际化消息。
 */
public final class ValidationMessageResolver {

    private static final String BUNDLE_NAME = "i18n.messages";
    private static final Pattern MESSAGE_KEY = Pattern.compile("^\\{([\\w.]+)}$");

    private ValidationMessageResolver() {
    }

    public static String resolve(ValidatorException exception, String acceptLanguage) {
        String message = exception.getMessage();
        Matcher matcher = MESSAGE_KEY.matcher(message == null ? "" : message);
        if (!matcher.matches()) {
            return message;
        }

        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, localeOf(acceptLanguage));
            String resolved = bundle.getString(matcher.group(1));
            Annotation annotation = validationAnnotation(exception);
            if (annotation instanceof Length length) {
                return resolved.replace("{min}", String.valueOf(length.min()))
                        .replace("{max}", String.valueOf(length.max()));
            }
            return resolved;
        } catch (MissingResourceException ignored) {
            return message;
        }
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

    private static Locale localeOf(String acceptLanguage) {
        if (acceptLanguage != null) {
            for (String languageRange : acceptLanguage.split(",")) {
                String language = languageRange.trim().toLowerCase(Locale.ROOT);
                if (language.startsWith("zh")) {
                    return Locale.SIMPLIFIED_CHINESE;
                }
                if (language.startsWith("en")) {
                    return Locale.US;
                }
            }
        }
        return Locale.SIMPLIFIED_CHINESE;
    }
}
