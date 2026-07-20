package com.jimuqu.system.translation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 系统字段翻译的值转换。 */
final class TranslationValueSupport {

    private TranslationValueSupport() {
    }

    static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static List<Long> distinctLongs(List<?> values) {
        return values.stream()
                .flatMap(value -> longIds(value).stream())
                .distinct()
                .toList();
    }

    static List<String> resolveLongValues(List<?> values, Map<Long, String> translations, String defaultValue) {
        return values.stream()
                .map(value -> {
                    String translated = longIds(value).stream()
                            .map(translations::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(","));
                    return translated.isEmpty() ? defaultValue : translated;
                })
                .toList();
    }

    private static List<Long> longIds(Object value) {
        if (value instanceof String text) {
            return java.util.Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isEmpty())
                    .map(TranslationValueSupport::toLong)
                    .filter(Objects::nonNull)
                    .toList();
        }
        Long id = toLong(value);
        return id == null ? List.of() : List.of(id);
    }
}
