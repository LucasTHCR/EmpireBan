package de.empireblocks.empireban.core.util;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static String translate(String input) {
        if (input == null) {
            return "";
        }
        return input.replace('&', '§');
    }
}
