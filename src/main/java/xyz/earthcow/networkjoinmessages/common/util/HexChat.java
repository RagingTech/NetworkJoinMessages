package xyz.earthcow.networkjoinmessages.common.util;

import java.util.regex.Pattern;

public class HexChat {

    public static final Pattern HEX_PATTERN = Pattern.compile(
        "&#(\\w{5}[0-9a-f])"
    );

    public static String translateHexCodes(String message) {
        // Translate RGB codes
        return message.replaceAll(
                            "(?i)\\&(x|#)([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])",
                            "&x&$2&$3&$4&$5&$6&$7"
                        );
    }

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile(
            "(?i)&([0-9A-FK-OR])|(?i)&#([0-9a-fA-F]{6})"
    );

    public static String removeColorCodes(String message) {
        return COLOR_CODE_PATTERN.matcher(message).replaceAll("");
    }
}
