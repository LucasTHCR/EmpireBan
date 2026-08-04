package de.empireblocks.empireban.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ids.yml stores raw seconds, but commands accept shorthand like "1d12h", "30m", "perm"
public final class TimeUtil {

    private static final Pattern SHORTHAND = Pattern.compile("(\\d+)([smhdwMy])");

    private TimeUtil() {
    }

    /** Returns seconds, or -1 for permanent. Returns -1 also on unparsable "perm"/"permanent". */
    public static long parseToSeconds(String input) {
        if (input == null) {
            return -1;
        }
        String trimmed = input.trim().toLowerCase();
        if (trimmed.equals("perm") || trimmed.equals("permanent") || trimmed.equals("-1")) {
            return -1;
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return Long.parseLong(trimmed);
        }
        Matcher matcher = SHORTHAND.matcher(trimmed);
        long totalSeconds = 0;
        boolean matchedAny = false;
        while (matcher.find()) {
            matchedAny = true;
            long amount = Long.parseLong(matcher.group(1));
            totalSeconds += switch (matcher.group(2)) {
                case "s" -> amount;
                case "m" -> amount * 60;
                case "h" -> amount * 3600;
                case "d" -> amount * 86400;
                case "w" -> amount * 604800;
                case "M" -> amount * 2592000L;
                case "y" -> amount * 31536000L;
                default -> 0;
            };
        }
        if (!matchedAny) {
            throw new IllegalArgumentException("Invalid time format: " + input);
        }
        return totalSeconds;
    }

    public static long secondsToExpiry(long seconds) {
        if (seconds < 0) {
            return -1;
        }
        return System.currentTimeMillis() + (seconds * 1000L);
    }

    public static String formatRemaining(long millis) {
        if (millis < 0) {
            return "Permanent";
        }
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86400;
        totalSeconds %= 86400;
        long hours = totalSeconds / 3600;
        totalSeconds %= 3600;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m ");
        }
        if (builder.isEmpty() || seconds > 0) {
            builder.append(seconds).append("s");
        }
        return builder.toString().trim();
    }
}
