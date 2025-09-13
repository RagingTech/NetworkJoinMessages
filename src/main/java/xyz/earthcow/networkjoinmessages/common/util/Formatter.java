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

    public Formatter(@NotNull CorePlugin plugin, @NotNull Storage storage) {
        this.storage = storage;
        // Get compatibility with other plugins, initialize hooks

        try {
            this.luckPerms = LuckPermsProvider.get();
            plugin.getCoreLogger().info("Successfully hooked into LuckPerms!");
        } catch (IllegalStateException | NoClassDefFoundError e) {
            plugin.getCoreLogger().warn("Could not find LuckPerms. Corresponding placeholders will be unavailable.");
        }

        try {
            this.placeholderAPI = PlaceholderAPI.createInstance();
            plugin.getCoreLogger().info("Successfully hooked into PAPIProxyBridge!");
        } catch (NoClassDefFoundError e) {
            plugin.getCoreLogger().warn("Could not find PAPIProxyBridge. Corresponding placeholders will be unavailable.");
        }

        if (plugin.isPluginLoaded("MiniPlaceholders")) {
            miniPlaceholders = new MiniPlaceholdersHook();
            plugin.getCoreLogger().info("Successfully hooked into MiniPlaceholders!");
        }

    }

    //region Legacy parsers
    private static String replaceEssentialsColorCodes(String str) {
        // "§x§f§b§6§3§f§5Hello!" -> "&#fb63f5Hello!"
        Matcher matcher = essentialsPattern.matcher(str);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String hexColor = matcher.group(0)
                    .replace("§x", "")
                    .replace("§", "");
            matcher.appendReplacement(result, "&#" + hexColor);
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private static String translateLegacyCodes(String str) {
        str = replaceEssentialsColorCodes(str);
        return str
                .replace('§', '&')
                .replace("&0", convertToTag(NamedTextColor.BLACK.asHexString()))
                .replace("&1", convertToTag(NamedTextColor.DARK_BLUE.asHexString()))
                .replace("&2", convertToTag(NamedTextColor.DARK_GREEN.asHexString()))
                .replace("&3", convertToTag(NamedTextColor.DARK_AQUA.asHexString()))
                .replace("&4", convertToTag(NamedTextColor.DARK_RED.asHexString()))
                .replace("&5", convertToTag(NamedTextColor.DARK_PURPLE.asHexString()))
                .replace("&6", convertToTag(NamedTextColor.GOLD.asHexString()))
                .replace("&7", convertToTag(NamedTextColor.GRAY.asHexString()))
                .replace("&8", convertToTag(NamedTextColor.DARK_GRAY.asHexString()))
                .replace("&9", convertToTag(NamedTextColor.BLUE.asHexString()))
                .replace("&a", convertToTag(NamedTextColor.GREEN.asHexString()))
                .replace("&b", convertToTag(NamedTextColor.AQUA.asHexString()))
                .replace("&c", convertToTag(NamedTextColor.RED.asHexString()))
                .replace("&d", convertToTag(NamedTextColor.LIGHT_PURPLE.asHexString()))
                .replace("&e", convertToTag(NamedTextColor.YELLOW.asHexString()))
                .replace("&f", convertToTag(NamedTextColor.WHITE.asHexString()))
                .replace("&k", convertToTag("obfuscated"))
                .replace("&l", convertToTag("bold"))
                .replace("&m", convertToTag("strikethrough"))
                .replace("&n", convertToTag("underlined"))
                .replace("&o", convertToTag("italic"))
                .replace("&r", convertToTag("reset"))
                .replace("\\n", convertToTag("newline"))

                // "&#FFC0CBHello! -> <#FFC0CB>Hello!
                .replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");
    }

    private static String convertToTag(String str) {
        return "<" + str + ">";
    }
    //endregion

    /**
     * Creates a component from the supplied string without parsing MiniPlaceholders
     * @param str String to construct a component from
     * @return An Adventure Component object
     */
    public static Component deserialize(String str) {
        return deserialize(str, null);
    }

    /**
     * Creates a component from the supplied string
     * @param str String to construct a component from
     * @param parseTarget Optional CorePlayer to parse MiniPlaceholder placeholders against
     * @return An Adventure Component object
     */
    public static Component deserialize(@NotNull String str, @Nullable CorePlayer parseTarget) {
        if (miniPlaceholders != null) {
            if (parseTarget == null) {
                return miniMessage.deserialize(translateLegacyCodes(str), miniPlaceholders.getGlobalResolver());
            } else {
                return miniMessage.deserialize(translateLegacyCodes(str), miniPlaceholders.getGlobalResolver(), miniPlaceholders.getAudienceResolver(parseTarget.getAudience()));
            }
        }
        return miniMessage.deserialize(translateLegacyCodes(str));
    }

    /**
     * Reverts the deserialization process
     * @param component The component to revert back to a string
     * @return A string theoretically capable of being deserialized back into an identical component as the input one
     */
    public static String serialize(Component component) {
        return miniMessage.serialize(component);
    }

    /**
     * Completely strip formatting and tags from a string
     * @param str The string to strip formatting and tags from
     * @return A new string containing no formatting or tags
     */
    public static String sanitize(String str) {
        return sanitize(deserialize(str));
    }

    /**
     * Completely strip formatting and tags from a component, converting it to a string
     * @param component The component to convert and strip formatting and tags from
     * @return A string containing no formatting or tags
     */
    public static String sanitize(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Parse LuckPerms placeholders, %player_prefix% & %player_suffix% from a string
     * @param str The string to parse
     * @param player The player to parse the string against
     * @return A string with parsed LuckPerms placeholders
     */
    private String handleLpPlaceholders(String str, CorePlayer player) {
        if (luckPerms == null) return str;
        User lpUser = luckPerms.getUserManager().getUser(player.getUniqueId());
        String prefix = "";
        String suffix = "";
        if (lpUser != null) {
            if (lpUser.getCachedData().getMetaData().getPrefix() != null) {
                prefix = lpUser.getCachedData().getMetaData().getPrefix();
            }
            if (lpUser.getCachedData().getMetaData().getSuffix() != null) {
                suffix = lpUser.getCachedData().getMetaData().getSuffix();
            }
        }
        return str
                .replace("%player_prefix%", prefix)
                .replace("%player_suffix%", suffix);
    }

    /**
     * This should be the final step before any player receives a message
     * Handles LuckPerms, MiniPlaceholders, and PlaceholderAPI placeholders
     * Handles custom placeholders: %player%, %displayname%, %server_name%, %server_name_clean%
     *
     * @param message The message to parse placeholders within
     * @param parseTarget The CorePlayer to provide replacements for placeholders
     * @param then The consumer to accept the parsed placeholder string
     */
    public void parsePlaceholdersAndThen(@NotNull String message, @NotNull CorePlayer parseTarget, Consumer<String> then) {
        message = handleLpPlaceholders(message, parseTarget)
                .replace("%player%", parseTarget.getName())
                .replace("%displayname%", parseTarget.getName())
                .replace("%server_name%", storage.getServerDisplayName(parseTarget.getCurrentServer().getName()))
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
}