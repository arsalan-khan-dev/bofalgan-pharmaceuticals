package com.bofalgan.pharmacy.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    public static final DateTimeFormatter DISPLAY_DATE     = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    public static final DateTimeFormatter DISPLAY_DATETIME = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
    public static final DateTimeFormatter DB_DATE          = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String format(LocalDate date) {
        if (date == null) return "";
        return date.format(DISPLAY_DATE);
    }

    public static String format(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DISPLAY_DATETIME);
    }

    public static String formatDb(LocalDate date) {
        if (date == null) return "";
        return date.format(DB_DATE);
    }

    public static LocalDate parseDisplayDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s, DISPLAY_DATE); } catch (Exception e) {}
        try { return LocalDate.parse(s, DB_DATE); } catch (Exception e) {}
        return null;
    }

    public static long daysUntil(LocalDate date) {
        if (date == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    public static String relativeTime(LocalDateTime dt) {
        if (dt == null) return "never";
        long seconds = ChronoUnit.SECONDS.between(dt, LocalDateTime.now());
        if (seconds < 60)   return seconds + "s ago";
        if (seconds < 3600) return (seconds / 60) + "m ago";
        if (seconds < 86400)return (seconds / 3600) + "h ago";
        return (seconds / 86400) + "d ago";
    }

    private DateUtils() {}
}
