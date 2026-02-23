package xyz.earthcow.networkjoinmessages.common.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.william278.papiproxybridge.api.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.modules.MiniPlaceholdersHook;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Formatter {

    private final Storage storage;

    private LuckPerms luckPerms = null;
    private PlaceholderAPI placeholderAPI = null;
    private static MiniPlaceholdersHook miniPlaceholders = null;

    public static final MiniMessage miniMessage = MiniMessage.miniMessage();
    public static final Pattern essentialsPattern = Pattern.compile("§x(§[0-9a-fA-F]){6}");

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

    public Formatter(@NotNull CorePlugin plugin, @NotNull Storage storage) {
        this(plugin, storage, 1500);
    }

    public Formatter(@NotNull CorePlugin plugin, @NotNull Storage storage, long ppbRequestTimeout) {
        this.storage = storage;

        if (plugin.isPluginLoaded("LuckPerms")) {
            try {
                this.luckPerms = LuckPermsProvider.get();
                plugin.getCoreLogger().info("Successfully hooked into LuckPerms!");
            } catch (IllegalStateException | NoClassDefFoundError ignored) {}
        }
        if (luckPerms == null) {
            plugin.getCoreLogger().warn("Could not find LuckPerms. Corresponding placeholders will be unavailable.");
        }

        if (plugin.isPluginLoaded("PAPIProxyBridge")) {
            try {
                this.placeholderAPI = PlaceholderAPI.createInstance();
                placeholderAPI.setRequestTimeout(ppbRequestTimeout);
                plugin.getCoreLogger().info("Successfully hooked into PAPIProxyBridge!");
            } catch (NoClassDefFoundError ignored) {}
        }
        if (placeholderAPI == null) {
            plugin.getCoreLogger().warn("Could not find PAPIProxyBridge. Corresponding placeholders will be unavailable.");
        }

        if (plugin.isPluginLoaded("MiniPlaceholders")) {
            miniPlaceholders = new MiniPlaceholdersHook();
            plugin.getCoreLogger().info("Successfully hooked into MiniPlaceholders!");
        }
    }

    //region Legacy parsers

    /**
     * Converts Essentials-style hex codes ({@code §x§f§b§6§3§f§5}) to the
     * Essentials shorthand ({@code &#fb63f5}).
     */
    private static String replaceEssentialsColorCodes(String str) {
        Matcher matcher = essentialsPattern.matcher(str);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String hexColor = matcher.group(0).replace("§x", "").replace("§", "");
            matcher.appendReplacement(result, "&#" + hexColor);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Converts legacy {@code &}-color codes and Essentials hex codes to MiniMessage tags.
     */
    private static String translateLegacyCodes(String str) {
        str = replaceEssentialsColorCodes(str).replace('§', '&');

        for (Map.Entry<String, String> entry : LEGACY_CODE_MAP.entrySet()) {
            str = str.replace(entry.getKey(), entry.getValue());
        }

        // "&#FFC0CBHello!" -> "<#FFC0CB>Hello!"
        return str.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");
    }

    private static String tag(String name) {
        return "<" + name + ">";
    }

    //endregion

    /**
     * Creates a component from the given string without parsing MiniPlaceholders.
     */
    public static Component deserialize(String str) {
        return deserialize(str, null);
    }

    /**
     * Creates a component from the given string, optionally parsing MiniPlaceholders.
     *
     * @param str         the string to deserialize
     * @param parseTarget optional player to parse MiniPlaceholder placeholders against
     */
    public static Component deserialize(@NotNull String str, @Nullable CorePlayer parseTarget) {
        String translated = translateLegacyCodes(str);
        if (miniPlaceholders != null) {
            if (parseTarget == null) {
                return miniMessage.deserialize(translated, miniPlaceholders.getGlobalResolver());
            } else {
                return miniMessage.deserialize(translated, parseTarget.getAudience(), miniPlaceholders.getAudienceGlobalResolver());
            }
        }
        return miniMessage.deserialize(translated);
    }

    /**
     * Serializes a component back to a MiniMessage string.
     */
    public static String serialize(Component component) {
        return miniMessage.serialize(component);
    }

    /**
     * Strips all formatting and tags from a string, returning plain text.
     */
    public static String sanitize(String str) {
        return sanitize(deserialize(str));
    }

    /**
     * Strips all formatting and tags from a component, returning plain text.
     */
    public static String sanitize(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Replaces {@code %player_prefix%} and {@code %player_suffix%} using LuckPerms data.
     */
    private String handleLpPlaceholders(String str, CorePlayer player) {
        if (luckPerms == null) return str;

        User lpUser = luckPerms.getUserManager().getUser(player.getUniqueId());
        String prefix = "";
        String suffix = "";
        if (lpUser != null) {
            String rawPrefix = lpUser.getCachedData().getMetaData().getPrefix();
            String rawSuffix = lpUser.getCachedData().getMetaData().getSuffix();
            if (rawPrefix != null) prefix = rawPrefix;
            if (rawSuffix != null) suffix = rawSuffix;
        }

        return str
            .replace("%player_prefix%", prefix)
            .replace("%player_suffix%", suffix);
    }

    /**
     * Fully resolves all placeholders in {@code message} and passes the result to {@code then}.
     *
     * <p>This should be the final step before any player receives a message. It handles:
     * LuckPerms, MiniPlaceholders, PAPIProxyBridge, and the built-in placeholders
     * ({@code %player%}, {@code %displayname%}, {@code %server_name%}, {@code %server_name_clean%}).
     *
     * @param message     the message template to parse
     * @param parseTarget the player to resolve placeholders against
     * @param then        callback accepting the fully resolved string
     */
    public void parsePlaceholdersAndThen(@NotNull String message, @NotNull CorePlayer parseTarget,
                                          Consumer<String> then) {
        message = handleLpPlaceholders(message, parseTarget)
            .replace("%player%",          parseTarget.getName())
            .replace("%displayname%",     parseTarget.getName())
            .replace("%server_name%",     storage.getServerDisplayName(parseTarget.getCurrentServer().getName()))
            .replace("%server_name_clean%", parseTarget.getCurrentServer().getName());

        if (miniPlaceholders != null) {
            message = serialize(deserialize(message, parseTarget));
        }

        if (placeholderAPI != null) {
            placeholderAPI.formatPlaceholders(message, parseTarget.getUniqueId()).thenAccept(then);
        } else {
            then.accept(message);
        }
    }

    public void setPPBRequestTimeout(long ppbRequestTimeout) {
        if (placeholderAPI != null) {
            placeholderAPI.setRequestTimeout(ppbRequestTimeout);
        }
    }
}
