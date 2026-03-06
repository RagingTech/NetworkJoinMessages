package xyz.earthcow.networkjoinmessages.common.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import lombok.Getter;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.util.SQLPlayerJoinTracker;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Holds all values read from config.yml and discord.yml.
 * Provides accessors for messages, feature flags, and server lists.
 * Has no knowledge of runtime player state.
 */
public final class PluginConfig {

    private final CorePlugin plugin;
    private final ConfigManager configManager;

    /** Map from real server name -> display server name */
    private final Map<String, String> serverDisplayNames = new HashMap<>();

    // Definite messages (used when non-empty; otherwise a random one is selected)
    private String swapServerMessage;
    private String firstJoinNetworkMessage;
    private String joinNetworkMessage;
    private String leaveNetworkMessage;

    // Randomized message pools
    private List<String> swapMessages;
    private List<String> firstJoinMessages;
    private List<String> joinMessages;
    private List<String> leaveMessages;

    // Command response messages
    @Getter private String noMoreArgumentsNeeded;
    @Getter private String noPermission;
    @Getter private String spoofNoArgument;
    @Getter private String spoofSwapNoArgument;
    @Getter private String spoofToggleSilentNoPerm;
    @Getter private String spoofToggleSilent;
    @Getter private String spoofJoinNotification;
    @Getter private String toggleJoinMissingFirstArg;
    @Getter private String toggleJoinMissingState;
    @Getter private String toggleJoinConfirmation;
    @Getter private String reloadConfirmation;
    @Getter private String silentPrefix;
    @Getter private String consoleSilentSwap;
    @Getter private String consoleSilentJoin;
    @Getter private String consoleSilentLeave;

    // Storage backend
    @Getter private String storageType;

    // SQL backend config (only used when StorageType is SQL)
    @Getter private String sqlHost;
    @Getter private int    sqlPort;
    @Getter private String sqlDatabase;
    @Getter private String sqlTablePrefix;
    @Getter private String sqlUsername;
    @Getter private String sqlPassword;
    @Getter private String sqlDriver;
    @Getter private boolean sqlUseSSL;
    @Getter private int    sqlConnectionTimeout;

    // Numeric settings
    @Getter private int leaveCacheDuration;
    @Getter private int leaveJoinBufferDuration;
    @Getter private long PPBRequestTimeout;

    /**
     * Default silent state for players with the {@code networkjoinmessages.silent} permission.
     * {@code true} = silent by default.
     */
    @Getter private boolean silentJoinDefaultState;

    // Feature enable/disable flags
    @Getter private boolean swapServerMessageEnabled;
    @Getter private boolean firstJoinNetworkMessageEnabled;
    @Getter private boolean joinNetworkMessageEnabled;
    @Getter private boolean leaveNetworkMessageEnabled;
    @Getter private boolean notifyAdminsOnSilentMove;

    // Message visibility per audience group
    @Getter private boolean swapViewableByJoined;
    @Getter private boolean swapViewableByLeft;
    @Getter private boolean swapViewableByOther;
    @Getter private boolean firstJoinViewableByJoined;
    @Getter private boolean firstJoinViewableByOther;
    @Getter private boolean joinViewableByJoined;
    @Getter private boolean joinViewableByOther;
    @Getter private boolean leaveViewableByLeft;
    @Getter private boolean leaveViewableByOther;

    // Per-server message suppression lists
    @Getter private List<String> serverFirstJoinMessageDisabled = new ArrayList<>();
    @Getter private List<String> serverJoinMessageDisabled = new ArrayList<>();
    @Getter private List<String> serverLeaveMessageDisabled = new ArrayList<>();

    // Blacklist/whitelist
    @Getter private List<String> blacklistedServers = new ArrayList<>();
    @Getter private boolean useBlacklistAsWhitelist;
    @Getter private String swapServerMessageRequires = "ANY";

    // Third-party plugin integration flags
    @Getter private boolean SVTreatVanishedPlayersAsSilent;
    @Getter private boolean SVRemoveVanishedPlayersFromPlayerCount;
    @Getter private boolean PVTreatVanishedPlayersAsSilent;
    @Getter private boolean PVRemoveVanishedPlayersFromPlayerCount;
    @Getter private boolean PVSpoofJoinMessageOnShow;
    @Getter private boolean PVSpoofLeaveMessageOnHide;
    @Getter private boolean PVTreatVanishedOnJoin;
    @Getter private boolean shouldSuppressLimboSwap;
    @Getter private boolean shouldSuppressLimboJoin;
    @Getter private boolean shouldSuppressLimboLeave;

    public PluginConfig(CorePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        reload();
    }

    /** Reloads all values from disk. */
    public void reload() {
        YamlDocument config = configManager.getPluginConfig();

        serverDisplayNames.clear();
        if (config.contains("Servers")) {
            for (String serverKey : config.getSection("Servers").getRoutesAsStrings(false)) {
                serverDisplayNames.put(serverKey, config.getString("Servers." + serverKey, serverKey));
            }
        }

        swapServerMessage       = config.getString("Messages.SwapServerMessage", "");
        firstJoinNetworkMessage = config.getString("Messages.FirstJoinNetworkMessage", "");
        joinNetworkMessage      = config.getString("Messages.JoinNetworkMessage", "");
        leaveNetworkMessage     = config.getString("Messages.LeaveNetworkMessage", "");

        swapMessages      = config.getStringList("Messages.SwapServerMessages");
        firstJoinMessages = config.getStringList("Messages.FirstJoinNetworkMessages");
        joinMessages      = config.getStringList("Messages.JoinNetworkMessages");
        leaveMessages     = config.getStringList("Messages.LeaveNetworkMessages");

        silentPrefix       = config.getString("Messages.Misc.SilentPrefix");
        consoleSilentSwap  = config.getString("Messages.Misc.ConsoleSilentSwap");
        consoleSilentJoin  = config.getString("Messages.Misc.ConsoleSilentJoin");
        consoleSilentLeave = config.getString("Messages.Misc.ConsoleSilentLeave");

        noMoreArgumentsNeeded     = config.getString("Messages.Commands.NoMoreArgumentsNeeded");
        noPermission              = config.getString("Messages.Commands.NoPermission");
        spoofNoArgument           = config.getString("Messages.Commands.Spoof.NoArgument");
        spoofSwapNoArgument       = config.getString("Messages.Commands.Spoof.SwapNoArgument");
        spoofToggleSilentNoPerm   = config.getString("Messages.Commands.Spoof.ToggleSilentNoPerm");
        spoofToggleSilent         = config.getString("Messages.Commands.Spoof.ToggleSilent");
        spoofJoinNotification     = config.getString("Messages.Commands.Spoof.JoinNotification");
        toggleJoinMissingFirstArg = config.getString("Messages.Commands.ToggleJoin.MissingFirstArgument");
        toggleJoinMissingState    = config.getString("Messages.Commands.ToggleJoin.MissingState");
        toggleJoinConfirmation    = config.getString("Messages.Commands.ToggleJoin.Confirmation");
        reloadConfirmation        = config.getString("Messages.Commands.Reload.ConfigReloaded");

        leaveCacheDuration      = config.getInt("Settings.LeaveNetworkMessageCacheDuration");
        leaveJoinBufferDuration = config.getInt("Settings.LeaveJoinBufferDuration");
        silentJoinDefaultState  = config.getBoolean("Settings.SilentJoinDefaultState");
        storageType             = config.getString("Settings.StorageType").toUpperCase();

        sqlHost                 = config.getString("Settings.SQL.Host");
        sqlPort                 = config.getInt("Settings.SQL.Port");
        sqlDatabase             = config.getString("Settings.SQL.Database");
        sqlTablePrefix          = config.getString("Settings.SQL.TablePrefix");
        sqlUsername             = config.getString("Settings.SQL.Username");
        sqlPassword             = config.getString("Settings.SQL.Password");
        sqlDriver               = config.getString("Settings.SQL.Driver").toLowerCase();
        sqlUseSSL               = config.getBoolean("Settings.SQL.UseSSL");
        sqlConnectionTimeout    = config.getInt("Settings.SQL.ConnectionTimeout");

        swapServerMessageEnabled       = config.getBoolean("Settings.SwapServerMessageEnabled");
        firstJoinNetworkMessageEnabled = config.getBoolean("Settings.FirstJoinNetworkMessageEnabled");
        joinNetworkMessageEnabled      = config.getBoolean("Settings.JoinNetworkMessageEnabled");
        leaveNetworkMessageEnabled     = config.getBoolean("Settings.LeaveNetworkMessageEnabled");
        notifyAdminsOnSilentMove       = config.getBoolean("Settings.NotifyAdminsOnSilentMove");

        swapViewableByJoined      = config.getBoolean("Settings.SwapServerMessageViewableBy.ServerJoined");
        swapViewableByLeft        = config.getBoolean("Settings.SwapServerMessageViewableBy.ServerLeft");
        swapViewableByOther       = config.getBoolean("Settings.SwapServerMessageViewableBy.OtherServer");
        firstJoinViewableByJoined = config.getBoolean("Settings.FirstJoinNetworkMessageViewableBy.ServerJoined");
        firstJoinViewableByOther  = config.getBoolean("Settings.FirstJoinNetworkMessageViewableBy.OtherServer");
        joinViewableByJoined      = config.getBoolean("Settings.JoinNetworkMessageViewableBy.ServerJoined");
        joinViewableByOther       = config.getBoolean("Settings.JoinNetworkMessageViewableBy.OtherServer");
        leaveViewableByLeft       = config.getBoolean("Settings.LeaveNetworkMessageViewableBy.ServerLeft");
        leaveViewableByOther      = config.getBoolean("Settings.LeaveNetworkMessageViewableBy.OtherServer");

        blacklistedServers        = config.getStringList("Settings.ServerBlacklist");
        useBlacklistAsWhitelist   = config.getBoolean("Settings.UseBlacklistAsWhitelist");
        swapServerMessageRequires = config.getString("Settings.SwapServerMessageRequires").toUpperCase();

        serverFirstJoinMessageDisabled = config.getStringList("Settings.IgnoreFirstJoinMessagesList");
        serverJoinMessageDisabled      = config.getStringList("Settings.IgnoreJoinMessagesList");
        serverLeaveMessageDisabled     = config.getStringList("Settings.IgnoreLeaveMessagesList");

        PPBRequestTimeout                      = config.getLong("OtherPlugins.PAPIProxyBridge.RequestTimeout");
        SVTreatVanishedPlayersAsSilent         = config.getBoolean("OtherPlugins.SayanVanish.TreatVanishedPlayersAsSilent");
        SVRemoveVanishedPlayersFromPlayerCount = config.getBoolean("OtherPlugins.SayanVanish.RemoveVanishedPlayersFromPlayerCount");
        PVTreatVanishedPlayersAsSilent         = config.getBoolean("OtherPlugins.PremiumVanish.TreatVanishedPlayersAsSilent");
        PVRemoveVanishedPlayersFromPlayerCount = config.getBoolean("OtherPlugins.PremiumVanish.RemoveVanishedPlayersFromPlayerCount");
        PVSpoofJoinMessageOnShow               = config.getBoolean("OtherPlugins.PremiumVanish.SpoofJoinMessageOnShow");
        PVSpoofLeaveMessageOnHide              = config.getBoolean("OtherPlugins.PremiumVanish.SpoofLeaveMessageOnHide");
        PVTreatVanishedOnJoin                  = config.getBoolean("OtherPlugins.PremiumVanish.TreatVanishedOnJoin");
        shouldSuppressLimboSwap                = config.getBoolean("OtherPlugins.LimboAPI.SuppressSwapMessages");
        shouldSuppressLimboJoin                = config.getBoolean("OtherPlugins.LimboAPI.SuppressJoinMessages");
        shouldSuppressLimboLeave               = config.getBoolean("OtherPlugins.LimboAPI.SuppressLeaveMessages");

        plugin.getCoreLogger().setDebug(config.getBoolean("debug"));

        validateConstrainedFields();
    }

    /** Validates fields with a constrained set of valid values and resets invalid ones. */
    private void validateConstrainedFields() {
        switch (storageType) {
            case "H2", "TEXT", "SQL" -> { /* valid */ }
            default -> {
                plugin.getCoreLogger().info(
                    "Setting error: Settings.StorageType only allows H2, TEXT, or SQL. " +
                    "Got '" + storageType + "'. Defaulting to H2."
                );
                storageType = "H2";
            }
        }
        if ("SQL".equals(storageType)) {
            switch (sqlDriver) {
                case "mysql", "mariadb", "postgresql" -> { /* valid */ }
                default -> {
                    plugin.getCoreLogger().info(
                        "Setting error: Settings.SQL.Driver only allows mysql, mariadb, or postgresql. " +
                        "Got '" + sqlDriver + "'. Defaulting to mysql."
                    );
                    sqlDriver = "mysql";
                }
            }
        }
        switch (swapServerMessageRequires) {
            case "JOINED", "LEFT", "BOTH", "ANY" -> { /* valid */ }
            default -> {
                plugin.getCoreLogger().info(
                    "Setting error: Settings.SwapServerMessageRequires only allows JOINED, LEFT, BOTH, or ANY. " +
                    "Got '" + swapServerMessageRequires + "'. Defaulting to ANY."
                );
                swapServerMessageRequires = "ANY";
            }
        }
        if (leaveCacheDuration < 0) {
            plugin.getCoreLogger().info(
                "Setting error: Settings.LeaveNetworkMessageCacheDuration requires a non-negative value. Defaulting to 0."
            );
            leaveCacheDuration = 0;
        }
        if (leaveJoinBufferDuration < 0) {
            plugin.getCoreLogger().info(
                "Setting error: Settings.LeaveJoinBufferDuration requires a non-negative value. Defaulting to 0."
            );
            leaveJoinBufferDuration = 0;
        }
    }

    // --- Server display name accessors ---

    public List<String> getServerNames() {
        return List.copyOf(serverDisplayNames.keySet());
    }

    public String getServerDisplayName(String serverName) {
        return serverDisplayNames.getOrDefault(serverName, serverName);
    }

    // --- Message accessors (select definite or random) ---

    public String getSwapServerMessage()       { return selectMessage(swapServerMessage,       swapMessages);      }
    public String getFirstJoinNetworkMessage() { return selectMessage(firstJoinNetworkMessage, firstJoinMessages); }
    public String getJoinNetworkMessage()      { return selectMessage(joinNetworkMessage,       joinMessages);      }
    public String getLeaveNetworkMessage()     { return selectMessage(leaveNetworkMessage,      leaveMessages);     }

    private String selectMessage(String definite, List<String> pool) {
        if (!definite.isEmpty()) return definite;
        if (pool.isEmpty())      return "";
        if (pool.size() == 1)    return pool.get(0);
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    // --- SQL config builder ---

    /**
     * Builds a {@link SQLPlayerJoinTracker.SQLConfig} from the values loaded during {@link #reload()}.
     * Only meaningful when {@link #getStorageType()} is {@code "SQL"}.
     */
    public SQLPlayerJoinTracker.SQLConfig buildSqlConfig() {
        return new SQLPlayerJoinTracker.SQLConfig(
            sqlHost, sqlPort, sqlDatabase,
            sqlUsername, sqlPassword,
            sqlDriver, sqlTablePrefix, sqlUseSSL, sqlConnectionTimeout
        );
    }

    // --- bStats charts ---

    public Collection<CustomChart> getCustomCharts() {
        List<CustomChart> charts = new ArrayList<>();
        YamlDocument defaults = Objects.requireNonNull(configManager.getPluginConfig().getDefaults());

        charts.add(new SimplePie("leave_cache_duration",         () -> String.valueOf(leaveCacheDuration)));
        charts.add(new SimplePie("storage_type",                  () -> storageType));
        charts.add(new SimplePie("swap_enabled",                 () -> String.valueOf(swapServerMessageEnabled)));
        charts.add(new SimplePie("first_join_enabled",           () -> String.valueOf(firstJoinNetworkMessageEnabled)));
        charts.add(new SimplePie("join_enabled",                 () -> String.valueOf(joinNetworkMessageEnabled)));
        charts.add(new SimplePie("leave_enabled",                () -> String.valueOf(leaveNetworkMessageEnabled)));
        charts.add(new SimplePie("random_swap",                  () -> String.valueOf(swapServerMessageEnabled && swapServerMessage.isEmpty())));
        charts.add(new SimplePie("random_first_join",            () -> String.valueOf(firstJoinNetworkMessageEnabled && firstJoinNetworkMessage.isEmpty())));
        charts.add(new SimplePie("random_join",                  () -> String.valueOf(joinNetworkMessageEnabled && joinNetworkMessage.isEmpty())));
        charts.add(new SimplePie("random_leave",                 () -> String.valueOf(leaveNetworkMessageEnabled && leaveNetworkMessage.isEmpty())));
        charts.add(new SimplePie("silent_join_default_state",    () -> String.valueOf(silentJoinDefaultState)));
        charts.add(new SimplePie("notify_admins_on_silent_move", () -> String.valueOf(notifyAdminsOnSilentMove)));

        charts.add(new SimplePie("swap_viewable_by", () -> {
            List<String> vals = new ArrayList<>();
            if (swapViewableByJoined) vals.add("joined");
            if (swapViewableByLeft)   vals.add("left");
            if (swapViewableByOther)  vals.add("other");
            if (vals.size() == 3) return "all";
            return vals.isEmpty() ? "none" : String.join(" ", vals);
        }));
        charts.add(new SimplePie("first_join_viewable_by", () -> {
            if (firstJoinViewableByJoined && firstJoinViewableByOther) return "all";
            if (firstJoinViewableByJoined) return "joined";
            if (firstJoinViewableByOther)  return "other";
            return "none";
        }));
        charts.add(new SimplePie("join_viewable_by", () -> {
            if (joinViewableByJoined && joinViewableByOther) return "all";
            if (joinViewableByJoined) return "joined";
            if (joinViewableByOther)  return "other";
            return "none";
        }));
        charts.add(new SimplePie("leave_viewable_by", () -> {
            if (leaveViewableByLeft && leaveViewableByOther) return "all";
            if (leaveViewableByLeft)  return "left";
            if (leaveViewableByOther) return "other";
            return "none";
        }));

        charts.add(new SimplePie("server_blacklist_is_default",
            () -> String.valueOf(blacklistedServers.equals(defaults.getStringList("Settings.ServerBlacklist")))));
        charts.add(new SimplePie("blacklist_is_whitelist",  () -> String.valueOf(useBlacklistAsWhitelist)));
        charts.add(new SimplePie("swap_requires",           () -> swapServerMessageRequires));
        charts.add(new SimplePie("ignore_first_join_list_is_default",
            () -> String.valueOf(serverFirstJoinMessageDisabled.equals(defaults.getStringList("Settings.IgnoreFirstJoinMessagesList")))));
        charts.add(new SimplePie("ignore_join_list_is_default",
            () -> String.valueOf(serverJoinMessageDisabled.equals(defaults.getStringList("Settings.IgnoreJoinMessagesList")))));
        charts.add(new SimplePie("ignore_leave_list_is_default",
            () -> String.valueOf(serverLeaveMessageDisabled.equals(defaults.getStringList("Settings.IgnoreLeaveMessagesList")))));

        charts.add(new SimplePie("sayan_vanish_vanished_are_silent",               () -> String.valueOf(SVTreatVanishedPlayersAsSilent)));
        charts.add(new SimplePie("sayan_vanish_remove_vanished_from_player_count", () -> String.valueOf(SVRemoveVanishedPlayersFromPlayerCount)));
        charts.add(new SimplePie("premium_vanish_vanished_are_silent",               () -> String.valueOf(PVTreatVanishedPlayersAsSilent)));
        charts.add(new SimplePie("premium_vanish_remove_vanished_from_player_count", () -> String.valueOf(PVRemoveVanishedPlayersFromPlayerCount)));
        charts.add(new SimplePie("premium_vanish_treat_vanished_on_join",            () -> String.valueOf(PVTreatVanishedOnJoin)));
        charts.add(new SimplePie("limbo_api_suppress_swap",  () -> String.valueOf(shouldSuppressLimboSwap)));
        charts.add(new SimplePie("limbo_api_suppress_join",  () -> String.valueOf(shouldSuppressLimboJoin)));
        charts.add(new SimplePie("limbo_api_suppress_leave", () -> String.valueOf(shouldSuppressLimboLeave)));

        charts.add(new SimplePie("sayan_vanish_is_present",      () -> String.valueOf(plugin.isPluginLoaded("SayanVanish"))));
        charts.add(new SimplePie("premium_vanish_is_present",    () -> String.valueOf(plugin.isPluginLoaded("PremiumVanish"))));
        charts.add(new SimplePie("limbo_api_is_present",         () -> String.valueOf(plugin.isPluginLoaded("LimboAPI"))));
        charts.add(new SimplePie("papi_bridge_is_present",       () -> String.valueOf(plugin.isPluginLoaded("PAPIProxyBridge"))));
        charts.add(new SimplePie("luck_perms_is_present",        () -> String.valueOf(plugin.isPluginLoaded("LuckPerms"))));
        charts.add(new SimplePie("mini_placeholders_is_present", () -> String.valueOf(plugin.isPluginLoaded("MiniPlaceholders"))));
        charts.add(new SimplePie("discord_integration_enabled",
            () -> String.valueOf(configManager.getDiscordConfig().getBoolean("Enabled"))));

        return charts;
    }
}
