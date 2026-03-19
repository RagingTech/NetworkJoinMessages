package xyz.earthcow.networkjoinmessages.common.util;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure utility for converting legacy Minecraft color codes (§ / & codes, Essentials hex)
 * to MiniMessage tags. Contains no state and no plugin dependencies.
 */
public final class LegacyColorTranslator {

    public static final Pattern ESSENTIALS_HEX_PATTERN = Pattern.compile("§x(§[0-9a-fA-F]){6}");

    /**
     * Ordered map of legacy {@code &}-code prefixes to their MiniMessage tag equivalents.
     * Hex color codes are handled separately via regex after this map is applied.
     */
    private static final Map<String, String> LEGACY_CODE_MAP = new LinkedHashMap<>();

    static {
        LEGACY_CODE_MAP.put("&0", tag(NamedTextColor.BLACK.asHexString()));
        LEGACY_CODE_MAP.put("&1", tag(NamedTextColor.DARK_BLUE.asHexString()));
        LEGACY_CODE_MAP.put("&2", tag(NamedTextColor.DARK_GREEN.asHexString()));
        LEGACY_CODE_MAP.put("&3", tag(NamedTextColor.DARK_AQUA.asHexString()));
        LEGACY_CODE_MAP.put("&4", tag(NamedTextColor.DARK_RED.asHexString()));
        LEGACY_CODE_MAP.put("&5", tag(NamedTextColor.DARK_PURPLE.asHexString()));
        LEGACY_CODE_MAP.put("&6", tag(NamedTextColor.GOLD.asHexString()));
        LEGACY_CODE_MAP.put("&7", tag(NamedTextColor.GRAY.asHexString()));
        LEGACY_CODE_MAP.put("&8", tag(NamedTextColor.DARK_GRAY.asHexString()));
        LEGACY_CODE_MAP.put("&9", tag(NamedTextColor.BLUE.asHexString()));
        LEGACY_CODE_MAP.put("&a", tag(NamedTextColor.GREEN.asHexString()));
        LEGACY_CODE_MAP.put("&b", tag(NamedTextColor.AQUA.asHexString()));
        LEGACY_CODE_MAP.put("&c", tag(NamedTextColor.RED.asHexString()));
        LEGACY_CODE_MAP.put("&d", tag(NamedTextColor.LIGHT_PURPLE.asHexString()));
        LEGACY_CODE_MAP.put("&e", tag(NamedTextColor.YELLOW.asHexString()));
        LEGACY_CODE_MAP.put("&f", tag(NamedTextColor.WHITE.asHexString()));
        LEGACY_CODE_MAP.put("&k", tag("obfuscated"));
        LEGACY_CODE_MAP.put("&l", tag("bold"));
        LEGACY_CODE_MAP.put("&m", tag("strikethrough"));
        LEGACY_CODE_MAP.put("&n", tag("underlined"));
        LEGACY_CODE_MAP.put("&o", tag("italic"));
        LEGACY_CODE_MAP.put("&r", tag("reset"));
        LEGACY_CODE_MAP.put("\\n", tag("newline"));
    }

    private LegacyColorTranslator() {}

    /**
     * Converts all legacy color codes (§, &, Essentials hex) in the given string to MiniMessage tags.
     */
    public static String translate(String str) {
        str = replaceEssentialsHex(str).replace('§', '&');
        for (Map.Entry<String, String> entry : LEGACY_CODE_MAP.entrySet()) {
            str = str.replace(entry.getKey(), entry.getValue());
        }
        // "&#FFC0CBHello!" -> "<#FFC0CB>Hello!"
        return str.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");
    }

    /**
     * Converts Essentials-style §-hex codes ({@code §x§f§b§6§3§f§5}) to shorthand ({@code &#fb63f5}).
     */
    private static String replaceEssentialsHex(String str) {
        Matcher matcher = ESSENTIALS_HEX_PATTERN.matcher(str);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(0).replace("§x", "").replace("§", "");
            matcher.appendReplacement(result, "&#" + hex);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String tag(String name) {
        return "<" + name + ">";
    }
}
