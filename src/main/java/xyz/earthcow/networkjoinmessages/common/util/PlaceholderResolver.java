package xyz.earthcow.networkjoinmessages.common.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.william278.papiproxybridge.api.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.modules.MiniPlaceholdersHook;

import java.util.function.Consumer;

/**
 * Resolves all runtime placeholders (%player%, %server_name%, LuckPerms, PAPI, MiniPlaceholders)
 * in a message string against a specific player.
 *
 * <p>This class owns the plugin-integration lifecycle (hook initialization, timeout configuration)
 * and is the only class that should call external placeholder APIs.
 */
public final class PlaceholderResolver {

    private final PluginConfig config;

    private LuckPerms luckPerms = null;
    private PlaceholderAPI placeholderAPI = null;
    private MiniPlaceholdersHook miniPlaceholders = null;

    public PlaceholderResolver(@NotNull CorePlugin plugin, @NotNull PluginConfig config) {
        this.config = config;
        initHooks(plugin);
    }

    private void initHooks(CorePlugin plugin) {
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
                placeholderAPI.setRequestTimeout(config.getPPBRequestTimeout());
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

    /**
     * Returns whether MiniPlaceholders is available. Used by {@link Formatter} for deserialization.
     */
    public MiniPlaceholdersHook getMiniPlaceholders() {
        return miniPlaceholders;
    }

    /**
     * Fully resolves all placeholders in {@code message} against {@code player}, then passes the
     * result to {@code callback}. The callback may be called asynchronously if PAPIProxyBridge is
     * in use.
     *
     * <p>Resolves in order:
     * <ol>
     *   <li>Built-in: %player%, %displayname%, %server_name%, %server_name_clean%</li>
     *   <li>LuckPerms: %player_prefix%, %player_suffix%</li>
     *   <li>MiniPlaceholders (serialized back to string)</li>
     *   <li>PAPIProxyBridge (async)</li>
     * </ol>
     */
    public void resolve(@NotNull String message, @NotNull CorePlayer player, Consumer<String> callback) {
        message = resolveBuiltins(message, player);
        message = resolveLuckPerms(message, player);

        if (miniPlaceholders != null) {
            message = Formatter.miniMessage.serialize(
                Formatter.miniMessage.deserialize(
                    LegacyColorTranslator.translate(message),
                    player.getAudience(),
                    miniPlaceholders.getAudienceGlobalResolver()
                )
            );
        }

        if (placeholderAPI != null) {
            placeholderAPI.formatPlaceholders(message, player.getUniqueId()).thenAccept(callback);
        } else {
            callback.accept(message);
        }
    }

    /** Replaces the four built-in placeholders provided by this plugin. */
    private String resolveBuiltins(String message, CorePlayer player) {
        return message
            .replace("%player%",            player.getName())
            .replace("%displayname%",       player.getName())
            .replace("%server_name%",       config.getServerDisplayName(player.getCurrentServer().getName()))
            .replace("%server_name_clean%", player.getCurrentServer().getName());
    }

    /** Replaces %player_prefix% and %player_suffix% using LuckPerms cached metadata. */
    private String resolveLuckPerms(String message, CorePlayer player) {
        if (luckPerms == null) return message;

        User lpUser = luckPerms.getUserManager().getUser(player.getUniqueId());
        String prefix = "";
        String suffix = "";
        if (lpUser != null) {
            String rawPrefix = lpUser.getCachedData().getMetaData().getPrefix();
            String rawSuffix = lpUser.getCachedData().getMetaData().getSuffix();
            if (rawPrefix != null) prefix = rawPrefix;
            if (rawSuffix != null) suffix = rawSuffix;
        }
        return message
            .replace("%player_prefix%", prefix)
            .replace("%player_suffix%", suffix);
    }

    public void setPPBRequestTimeout(long timeoutMs) {
        if (placeholderAPI != null) {
            placeholderAPI.setRequestTimeout(timeoutMs);
        }
    }
}
