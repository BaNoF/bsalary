package ru.mine.banoff.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");
    private static final String VALID_COLOR_CHARS = "0123456789abcdefklmnorx";
    public static final char COLOR_CHAR = '§';

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        final Matcher matcher = HEX_PATTERN.matcher(message);
        final StringBuilder builder = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(builder,
                    COLOR_CHAR + "x" +
                            COLOR_CHAR + group.charAt(0) +
                            COLOR_CHAR + group.charAt(1) +
                            COLOR_CHAR + group.charAt(2) +
                            COLOR_CHAR + group.charAt(3) +
                            COLOR_CHAR + group.charAt(4) +
                            COLOR_CHAR + group.charAt(5));
        }
        message = matcher.appendTail(builder).toString();
        return translateAlternateColorCodes('&', message);
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        if (textToTranslate == null) {
            return null;
        }

        final char[] b = textToTranslate.toCharArray();

        for (int i = 0, length = b.length - 1; i < length; ++i) {
            if (b[i] == altColorChar && isValidColorCharacter(b[i + 1])) {
                b[i++] = COLOR_CHAR;
                b[i] = Character.toLowerCase(b[i]);
            }
        }

        return new String(b);
    }
    private static boolean isValidColorCharacter(char c) {
        return VALID_COLOR_CHARS.indexOf(Character.toLowerCase(c)) != -1;
    }
}
