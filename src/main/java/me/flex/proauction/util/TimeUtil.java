package me.flex.proauction.util;

public final class TimeUtil {
    private TimeUtil() {}

    public static String formatDurationShort(long millis) {
        if (millis <= 0) return "Expired";

        long seconds = millis / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    public static String formatAgoShort(long millisAgo) {
        if (millisAgo < 0) millisAgo = 0;
        return formatDurationShort(millisAgo) + " ago";
    }
}
