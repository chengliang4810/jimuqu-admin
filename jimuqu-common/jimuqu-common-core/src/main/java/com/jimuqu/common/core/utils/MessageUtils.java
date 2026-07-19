package com.jimuqu.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.noear.solon.core.handle.Context;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 获取国际化资源消息。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageUtils {

    private static final String BUNDLE_NAME = "i18n.messages";
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);

    /**
     * 根据当前请求语言解析消息键；非消息键按普通模板原样兼容。
     */
    public static String message(String code, Object... args) {
        return message(code, currentLocale(), args);
    }

    /**
     * 根据指定语言解析消息键，便于校验器与非请求线程复用。
     */
    public static String message(String code, Locale locale, Object... args) {
        if (StringUtil.isBlank(code)) {
            return code;
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME,
                    locale == null ? Locale.SIMPLIFIED_CHINESE : locale,
                    NO_FALLBACK_CONTROL);
            String pattern = bundle.getString(code);
            return args == null || args.length == 0
                    ? pattern
                    : new MessageFormat(pattern, bundle.getLocale()).format(args);
        } catch (MissingResourceException | IllegalArgumentException ignored) {
            return StringUtil.format(code, args);
        }
    }

    /**
     * Bell 请求优先使用 Content-Language，缺失时兼容 Accept-Language。
     */
    public static Locale resolveLocale(String contentLanguage, String acceptLanguage) {
        String language = StringUtil.isBlank(contentLanguage) ? acceptLanguage : contentLanguage;
        if (language != null) {
            for (String languageRange : language.split(",")) {
                String normalized = languageRange.trim().toLowerCase(Locale.ROOT);
                if (normalized.startsWith("zh")) {
                    return Locale.SIMPLIFIED_CHINESE;
                }
                if (normalized.startsWith("en")) {
                    return Locale.US;
                }
            }
        }
        return Locale.SIMPLIFIED_CHINESE;
    }

    private static Locale currentLocale() {
        try {
            Context context = Context.current();
            if (context != null) {
                return resolveLocale(context.header("Content-Language"), context.header("Accept-Language"));
            }
        } catch (RuntimeException ignored) {
            // 非 HTTP 线程使用中文默认资源。
        }
        return Locale.SIMPLIFIED_CHINESE;
    }
}
