package com.tzly.phone.call;

import java.util.regex.Pattern;

public class RegexUtils {
    private RegexUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static boolean isMobileSimple(final CharSequence input) {
        return isMatch("^[1]\\d{10}$", input);
    }

    public static boolean isNumber(final CharSequence input) {
        return isMatch("^[0-9]\\d+$", input);
    }

    public static boolean isMatch(final String regex, final CharSequence input) {
        return input != null && input.length() > 0 && Pattern.matches(regex, input);
    }
}
