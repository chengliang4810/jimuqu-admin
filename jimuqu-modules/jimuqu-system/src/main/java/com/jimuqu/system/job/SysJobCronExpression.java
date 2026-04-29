package com.jimuqu.system.job;

import com.jimuqu.common.core.utils.StringUtil;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 轻量 cron 表达式，仅用于进程内任务下一次触发时间计算。
 * 支持 5 位或 6 位表达式，以及 *、?、,、-、/。
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
public class SysJobCronExpression {

    private static final long MAX_SEARCH_SECONDS = 366L * 24L * 60L * 60L;

    private final Set<Integer> seconds;
    private final Set<Integer> minutes;
    private final Set<Integer> hours;
    private final Set<Integer> days;
    private final Set<Integer> months;
    private final Set<Integer> weeks;

    public SysJobCronExpression(String expression) {
        if (StringUtil.isBlank(expression)) {
            throw new IllegalArgumentException("cron表达式不能为空");
        }
        String[] fields = expression.trim().split("\\s+");
        if (fields.length == 5) {
            this.seconds = Set.of(0);
            this.minutes = parseField(fields[0], 0, 59, null);
            this.hours = parseField(fields[1], 0, 23, null);
            this.days = parseField(fields[2], 1, 31, null);
            this.months = parseField(fields[3], 1, 12, monthAlias());
            this.weeks = parseField(fields[4], 0, 7, weekAlias());
        } else if (fields.length == 6) {
            this.seconds = parseField(fields[0], 0, 59, null);
            this.minutes = parseField(fields[1], 0, 59, null);
            this.hours = parseField(fields[2], 0, 23, null);
            this.days = parseField(fields[3], 1, 31, null);
            this.months = parseField(fields[4], 1, 12, monthAlias());
            this.weeks = parseField(fields[5], 0, 7, weekAlias());
        } else {
            throw new IllegalArgumentException("cron表达式仅支持5位或6位: " + expression);
        }
    }

    /**
     * 计算下一次触发时间。
     */
    public LocalDateTime nextTimeAfter(LocalDateTime after) {
        LocalDateTime cursor = after.plusSeconds(1).withNano(0);
        for (long i = 0; i < MAX_SEARCH_SECONDS; i++) {
            if (matches(cursor)) {
                return cursor;
            }
            cursor = cursor.plusSeconds(1);
        }
        throw new IllegalArgumentException("一年内无法找到cron下一次触发时间");
    }

    private boolean matches(LocalDateTime time) {
        int week = toCronWeek(time.getDayOfWeek());
        return seconds.contains(time.getSecond())
                && minutes.contains(time.getMinute())
                && hours.contains(time.getHour())
                && days.contains(time.getDayOfMonth())
                && months.contains(time.getMonthValue())
                && (weeks.contains(week) || weeks.contains(7) && week == 0);
    }

    private Set<Integer> parseField(String field, int min, int max, java.util.Map<String, Integer> aliases) {
        Set<Integer> result = new HashSet<>();
        for (String part : field.split(",")) {
            parsePart(part.trim(), min, max, aliases, result);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("cron字段无有效值: " + field);
        }
        return result;
    }

    private void parsePart(String part, int min, int max, java.util.Map<String, Integer> aliases, Set<Integer> result) {
        if (StringUtil.isBlank(part) || "?".equals(part)) {
            fillRange(min, max, 1, result);
            return;
        }
        String[] stepSplit = part.split("/");
        String rangePart = stepSplit[0];
        int step = stepSplit.length == 2 ? Integer.parseInt(stepSplit[1]) : 1;
        if (step <= 0 || stepSplit.length > 2) {
            throw new IllegalArgumentException("cron步长不合法: " + part);
        }
        if ("*".equals(rangePart) || "?".equals(rangePart)) {
            fillRange(min, max, step, result);
            return;
        }
        if (rangePart.contains("-")) {
            String[] range = rangePart.split("-");
            if (range.length != 2) {
                throw new IllegalArgumentException("cron范围不合法: " + part);
            }
            fillRange(parseValue(range[0], aliases), parseValue(range[1], aliases), step, result);
            return;
        }
        int value = parseValue(rangePart, aliases);
        checkRange(value, min, max);
        if (stepSplit.length == 2) {
            fillRange(value, max, step, result);
        } else {
            result.add(value);
        }
    }

    private void fillRange(int start, int end, int step, Set<Integer> result) {
        if (start > end) {
            throw new IllegalArgumentException("cron范围开始值不能大于结束值");
        }
        for (int i = start; i <= end; i += step) {
            result.add(i);
        }
    }

    private int parseValue(String value, java.util.Map<String, Integer> aliases) {
        String normalized = value.toUpperCase(Locale.ROOT);
        if (aliases != null && aliases.containsKey(normalized)) {
            return aliases.get(normalized);
        }
        return Integer.parseInt(value);
    }

    private void checkRange(int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("cron字段值超出范围: " + value);
        }
    }

    private int toCronWeek(DayOfWeek dayOfWeek) {
        return dayOfWeek.getValue() % 7;
    }

    private java.util.Map<String, Integer> monthAlias() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("JAN", 1),
                java.util.Map.entry("FEB", 2),
                java.util.Map.entry("MAR", 3),
                java.util.Map.entry("APR", 4),
                java.util.Map.entry("MAY", 5),
                java.util.Map.entry("JUN", 6),
                java.util.Map.entry("JUL", 7),
                java.util.Map.entry("AUG", 8),
                java.util.Map.entry("SEP", 9),
                java.util.Map.entry("OCT", 10),
                java.util.Map.entry("NOV", 11),
                java.util.Map.entry("DEC", 12)
        );
    }

    private java.util.Map<String, Integer> weekAlias() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("SUN", 0),
                java.util.Map.entry("MON", 1),
                java.util.Map.entry("TUE", 2),
                java.util.Map.entry("WED", 3),
                java.util.Map.entry("THU", 4),
                java.util.Map.entry("FRI", 5),
                java.util.Map.entry("SAT", 6)
        );
    }
}
