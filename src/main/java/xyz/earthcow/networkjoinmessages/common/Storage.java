package xyz.earthcow.networkjoinmessages.common;

import dev.dejvokep.boostedyaml.YamlDocument;
import lombok.Getter;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class for holding config values and user data that should persist after the user leaves the proxy.
 */
public final class Storage {

    private final CorePlugin plugin;
    private final ConfigManager configManager;

    // Per-player state that persists across server switches
    private final Map<UUID, String> previousServer = new HashMap<>();
    private final Map<UUID, Boolean> messageState = new HashMap<>();

    // Using Set<UUID> for O(1) membership tests (vs O(n) with List)
    private final Set<UUID> onlinePlayers = new HashSet<>();
    private final Set<UUID> noJoinMessage = new HashSet<>();
    private final Set<UUID> noLeaveMessage = new HashSet<>();
    private final Set<UUID> noSwapMessage = new HashSet<>();

    //region Configuration fields

    /** Map from real server name (lowercase) -> display server name */
    private final Map<String, String> serverDisplayNames = new HashMap<>();

    // Definite messages (used when non-empty; otherwise a random one is picked)
    private String swapServerMessage;
    private String firstJoinNetworkMessage;
    private String joinNetworkMessage;
    private String leaveNetworkMessage;

    // Randomized message pools
    private List<String> swapMessages;
    private List<String> firstJoinMessages;
    private List<String> joinMessages;
    private List<String> leaveMessages;

    // Command messages
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
    @Getter private int leaveCacheDuration;
    @Getter private int leaveJoinBufferDuration;

    /**
     * Default silent state for players joining with the {@code networkjoinmessages.silent} permission.
     * {@code true} means silent by default (no join message).
     */
    private boolean silentJoinDefaultState;

    @Getter private boolean swapServerMessageEnabled;
    @Getter private boolean firstJoinNetworkMessageEnabled;
    @Getter private boolean joinNetworkMessageEnabled;
    @Getter private boolean leaveNetworkMessageEnabled;
    @Getter private boolean notifyAdminsOnSilentMove;

    private boolean swapViewableByJoined;
    private boolean swapViewableByLeft;
    private boolean swapViewableByOther;
    private boolean firstJoinViewableByJoined;
    private boolean firstJoinViewableByOther;
    private boolean joinViewableByJoined;
    private boolean joinViewableByOther;
    private boolean leaveViewableByLeft;
    private boolean leaveViewableByOther;

    private List<String> serverFirstJoinMessageDisabled = new ArrayList<>();
    private List<String> serverJoinMessageDisabled = new ArrayList<>();
    private List<String> serverLeaveMessageDisabled = new ArrayList<>();

    private List<String> blacklistedServers = new ArrayList<>();
    private boolean useBlacklistAsWhitelist;
    private String swapServerMessageRequires = "ANY";

    // Third-party plugin integration flags
    @Getter private long PPBRequestTimeout;
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

    //endregion

    public Storage(@NotNull CorePlugin plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        setUpDefaultValuesFromConfig();
    }

    /** Loads all values from the plugin config file into this storage object. */
    public void setUpDefaultValuesFromConfig() {
        YamlDocument config = configManager.getPluginConfig();

        serverDisplayNames.clear();
        for (String serverKey : config.getSection("Servers").getRoutesAsStrings(false)) {
            serverDisplayNames.put(serverKey.toLowerCase(), config.getString("Servers." + serverKey, serverKey));
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

        blacklistedServers      = config.getStringList("Settings.ServerBlacklist");
        useBlacklistAsWhitelist = config.getBoolean("Settings.UseBlacklistAsWhitelist");
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

        validateConfig();
    }

    /**
     * Validates constrained config fields and resets invalid values to safe defaults.
     */
    private void validateConfig() {
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

    public boolean getSilentMessageState(CorePlayer player) {
        if (!player.hasPermission("networkjoinmessages.silent")) return false;
        // computeIfAbsent avoids a redundant map lookup compared to containsKey + get + put
        return messageState.computeIfAbsent(player.getUniqueId(), id -> silentJoinDefaultState);
    }

    public void setSilentMessageState(CorePlayer player, boolean state) {
        messageState.put(player.getUniqueId(), state);
    }

    public boolean isConnected(CorePlayer player) {
        return onlinePlayers.contains(player.getUniqueId());
    }

    public void setConnected(CorePlayer player, boolean state) {
        if (state) {
            onlinePlayers.add(player.getUniqueId());
        } else {
            onlinePlayers.remove(player.getUniqueId());
        }
    }

    public String getFrom(CorePlayer player) {
        return previousServer.getOrDefault(player.getUniqueId(), player.getCurrentServer().getName());
    }

    public void setFrom(CorePlayer player, String name) {
        previousServer.put(player.getUniqueId(), name);
    }

    private void setJoinState(UUID id, boolean state)  { if (state) noJoinMessage.remove(id);  else noJoinMessage.add(id);  }
    private void setLeaveState(UUID id, boolean state) { if (state) noLeaveMessage.remove(id); else noLeaveMessage.add(id); }
    private void setSwapState(UUID id, boolean state)  { if (state) noSwapMessage.remove(id);  else noSwapMessage.add(id);  }

    public void setSendMessageState(String list, UUID id, boolean state) {
        switch (list) {
            case "all"   -> { setSwapState(id, state); setJoinState(id, state); setLeaveState(id, state); }
            case "join"  -> setJoinState(id, state);
            case "leave" -> setLeaveState(id, state);
            case "swap"  -> setSwapState(id, state);
        }
    }

    public Set<UUID> getIgnorePlayers(MessageType type) {
        return switch (type) {
            case JOIN, FIRST_JOIN -> noJoinMessage;
            case SWAP             -> noSwapMessage;
            case LEAVE            -> noLeaveMessage;
        };
    }

    /**
     * Determines which players should receive a message based on configured visibility flags.
     *
     * @param viewableByJoined whether players on the "joined" server see the message
     * @param viewableByLeft   whether players on the "left" server see the message (ignored if {@code fromServer} is null)
     * @param viewableByOther  whether players on all other servers see the message
     * @param toServer         name of the destination server, or null if not applicable
     * @param fromServer       name of the origin server, or null if not applicable
     * @return mutable list of players who should receive the message
     */
    private List<CorePlayer> getReceivers(
            boolean viewableByJoined,
            boolean viewableByLeft,
            boolean viewableByOther,
            String toServer,
            String fromServer
    ) {
        if (viewableByJoined && (viewableByLeft || fromServer == null) && viewableByOther) {
            return new ArrayList<>(plugin.getAllPlayers());
        }

        if (viewableByOther) {
            List<CorePlayer> receivers = new ArrayList<>(plugin.getAllPlayers());
            if (!viewableByJoined && toServer != null)   receivers.removeAll(getServerPlayers(toServer));
            if (!viewableByLeft   && fromServer != null) receivers.removeAll(getServerPlayers(fromServer));
            return receivers;
        }

        List<CorePlayer> receivers = new ArrayList<>();
        if (viewableByJoined && toServer != null)   receivers.addAll(getServerPlayers(toServer));
        if (viewableByLeft   && fromServer != null) receivers.addAll(getServerPlayers(fromServer));
        return receivers;
    }

    public List<CorePlayer> getSwapMessageReceivers(String to, String from) {
        return getReceivers(swapViewableByJoined, swapViewableByLeft, swapViewableByOther, to, from);
    }

    public List<CorePlayer> getFirstJoinMessageReceivers(String server) {
        return getReceivers(firstJoinViewableByJoined, true, firstJoinViewableByOther, server, null);
    }

    public List<CorePlayer> getJoinMessageReceivers(String server) {
        return getReceivers(joinViewableByJoined, true, joinViewableByOther, server, null);
    }

    public List<CorePlayer> getLeaveMessageReceivers(String server) {
        return getReceivers(leaveViewableByLeft, true, leaveViewableByOther, server, null);
    }

    public List<CorePlayer> getServerPlayers(String serverName) {
        CoreBackendServer backendServer = plugin.getServer(serverName);
        return backendServer != null ? backendServer.getPlayersConnected() : Collections.emptyList();
    }

    /**
     * Returns true if the given player's current server is blocked by the blacklist/whitelist rules.
     */
    public boolean isBlacklisted(CorePlayer player) {
        String server = player.getCurrentServer().getName();
        boolean listed = blacklistedServers.contains(server);
        boolean result = useBlacklistAsWhitelist != listed;
        plugin.getCoreLogger().debug(String.format(
            "Blacklist check for player %s on server %s: listed=%s, mode=%s, result=%s",
            player.getName(), server, listed,
            useBlacklistAsWhitelist ? "WHITELIST" : "BLACKLIST", result
        ));
        return result;
    }

    /**
     * Returns true if the given server pair is blocked by the blacklist/whitelist + swapServerMessageRequires rules.
     */
    public boolean isBlacklisted(@Nullable String from, @Nullable String to) {
        boolean fromListed = from != null && blacklistedServers.contains(from);
        boolean toListed   = to   != null && blacklistedServers.contains(to);

        boolean result = switch (swapServerMessageRequires) {
            case "JOINED" -> toListed;
            case "LEFT"   -> fromListed;
            case "ANY"    -> fromListed || toListed;
            case "BOTH"   -> fromListed && toListed;
            default -> {
                plugin.getCoreLogger().warn("Unrecognized swapServerMessageRequires value: " + swapServerMessageRequires);
                yield false;
            }
        };

        boolean finalResult = useBlacklistAsWhitelist != result;
        plugin.getCoreLogger().debug(String.format(
            "Blacklist check for swap (from=%s, to=%s): fromListed=%s, toListed=%s, mode=%s, requires=%s, result=%s",
            from, to, fromListed, toListed,
            useBlacklistAsWhitelist ? "WHITELIST" : "BLACKLIST",
            swapServerMessageRequires, finalResult
        ));
        return finalResult;
    }

    /**
     * Returns the UUIDs of all players on servers where messages of the given type are disabled.
     */
    public Set<UUID> getIgnoredServerPlayers(MessageType type) {
        List<String> disabledServers = switch (type) {
            case FIRST_JOIN -> serverFirstJoinMessageDisabled;
            case JOIN       -> serverJoinMessageDisabled;
            case LEAVE      -> serverLeaveMessageDisabled;
            default -> {
                plugin.getCoreLogger().debug("No ignored servers defined for message type: " + type);
                yield Collections.emptyList();
            }
        };

        Set<UUID> ignored = new HashSet<>();
        for (String serverName : disabledServers) {
            CoreBackendServer backendServer = plugin.getServer(serverName);
            if (backendServer != null) {
                backendServer.getPlayersConnected().forEach(p -> ignored.add(p.getUniqueId()));
            } else {
                plugin.getCoreLogger().debug("Ignored server not found or offline: " + serverName);
            }
        }
        return ignored;
    }

    private String getRandomMessage(List<String> messageList) {
        if (messageList.isEmpty()) return "";
        if (messageList.size() == 1) return messageList.get(0);
        return messageList.get(ThreadLocalRandom.current().nextInt(messageList.size()));
    }

    private String getMessage(String message, List<String> messageList) {
        return message.isEmpty() ? getRandomMessage(messageList) : message;
    }

    //region Getters

    public List<String> getServerNames() {
        return List.copyOf(serverDisplayNames.keySet());
    }

    public String getServerDisplayName(String serverName) {
        return serverDisplayNames.getOrDefault(serverName, serverName);
    }

    public String getSwapServerMessage()       { return getMessage(swapServerMessage,       swapMessages);      }
    public String getFirstJoinNetworkMessage() { return getMessage(firstJoinNetworkMessage, firstJoinMessages); }
    public String getJoinNetworkMessage()      { return getMessage(joinNetworkMessage,       joinMessages);      }
    public String getLeaveNetworkMessage()     { return getMessage(leaveNetworkMessage,      leaveMessages);     }

    public Collection<CustomChart> getCustomCharts() {
        List<CustomChart> charts = new ArrayList<>();
        YamlDocument defaults = Objects.requireNonNull(configManager.getPluginConfig().getDefaults());

        charts.add(new SimplePie("leave_cache_duration",         () -> String.valueOf(leaveCacheDuration)));
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
            if (vals.isEmpty())   return "none";
            return String.join(" ", vals);
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
        charts.add(new SimplePie("blacklist_is_whitelist", () -> String.valueOf(useBlacklistAsWhitelist)));
        charts.add(new SimplePie("swap_requires",          () -> swapServerMessageRequires));
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

        charts.add(new SimplePie("sayan_vanish_is_present",     () -> String.valueOf(plugin.isPluginLoaded("SayanVanish"))));
        charts.add(new SimplePie("premium_vanish_is_present",   () -> String.valueOf(plugin.isPluginLoaded("PremiumVanish"))));
        charts.add(new SimplePie("limbo_api_is_present",        () -> String.valueOf(plugin.isPluginLoaded("LimboAPI"))));
        charts.add(new SimplePie("papi_bridge_is_present",      () -> String.valueOf(plugin.isPluginLoaded("PAPIProxyBridge"))));
        charts.add(new SimplePie("luck_perms_is_present",       () -> String.valueOf(plugin.isPluginLoaded("LuckPerms"))));
        charts.add(new SimplePie("mini_placeholders_is_present",() -> String.valueOf(plugin.isPluginLoaded("MiniPlaceholders"))));
        charts.add(new SimplePie("discord_integration_enabled",
            () -> String.valueOf(configManager.getDiscordConfig().getBoolean("Enabled"))));

        return charts;
    }

    //endregion
}
