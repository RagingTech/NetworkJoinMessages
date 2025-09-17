package xyz.earthcow.networkjoinmessages.common;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.*;

/**
 * Singleton class for holding config values and user data that should persist after the user leaves the proxy
 */
public final class Storage {

    private final CorePlugin plugin;

    // User data that should persist after they leave
    // User data that shouldn't persist after they leave can be stored in CorePlayer
    private final HashMap<UUID, String> previousServer = new HashMap<>();
    private final HashMap<UUID, Boolean> messageState = new HashMap<>();
    private final List<UUID> onlinePlayers = new ArrayList<>();
    private final List<UUID> noJoinMessage = new ArrayList<>();
    private final List<UUID> noLeaveMessage = new ArrayList<>();
    private final List<UUID> noSwitchMessage = new ArrayList<>();

    //region Configuration fields

    /**
     * Map from valid server name -> display server name
     */
    private final HashMap<String, String> serverDisplayNames = new HashMap<>();

    // Definite messages
    private String swapServerMessage;
    private String firstJoinNetworkMessage;
    private String joinNetworkMessage;
    private String leaveNetworkMessage;

    // Randomized messages - enabled if the definite message is empty
    private List<String> swapMessages;
    private List<String> firstJoinMessages;
    private List<String> joinMessages;
    private List<String> leaveMessages;

    private String silentPrefix;
    private String consoleSilentSwap;
    private String consoleSilentJoin;
    private String consoleSilentLeave;

    /**
     * The default silent state of a player joining with the networkjoinmessages.silent permission
     * Default: true - Someone joining with the permission will be silent (not send a join message)
     */
    private boolean silentJoinDefaultState;

    // Whether specific message types are enabled
    private boolean swapServerMessageEnabled;
    private boolean firstJoinNetworkMessageEnabled;
    private boolean joinNetworkMessageEnabled;
    private boolean leaveNetworkMessageEnabled;

    private boolean notifyAdminsOnSilentMove;

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

    // BlackList settings
    private List<String> blacklistedServers = new ArrayList<>();
    private boolean useBlacklistAsWhitelist;
    private String swapServerMessageRequires = "ANY";

    /// Other plugins
    // PremiumVanish
    private boolean treatVanishedPlayersAsSilent;
    private boolean removeVanishedPlayersFromPlayerCount;
    // LimboAPI
    private boolean shouldSuppressLimboSwap;
    private boolean shouldSuppressLimboJoin;
    private boolean shouldSuppressLimboLeave;

    /// Debug
    private boolean debug;

    //endregion

    public Storage(@NotNull CorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Grab values from config and save them here
     */
    public void setUpDefaultValuesFromConfig() {
        YamlDocument config = ConfigManager.getPluginConfig();

        // Load server display names using their real names as defaults
        for (String serverKey : config.getSection("Servers").getRoutesAsStrings(false)) {
            this.serverDisplayNames.put(
                serverKey.toLowerCase(),
                config.getString("Servers." + serverKey, serverKey)
            );
        }

        /// Messages

        // Set definite messages
        this.swapServerMessage = config.getString("Messages.SwapServerMessage", "");
        this.firstJoinNetworkMessage = config.getString("Messages.FirstJoinNetworkMessage", "");
        this.joinNetworkMessage = config.getString("Messages.JoinNetworkMessage", "");
        this.leaveNetworkMessage = config.getString("Messages.LeaveNetworkMessage", "");

        // Set randomized messages
        this.swapMessages = config.getStringList("Messages.SwapServerMessages");
        this.firstJoinMessages = config.getStringList("Messages.FirstJoinNetworkMessages");
        this.joinMessages = config.getStringList("Messages.JoinNetworkMessages");
        this.leaveMessages = config.getStringList("Messages.LeaveNetworkMessages");

        this.silentPrefix = config.getString("Messages.Misc.SilentPrefix");
        this.consoleSilentSwap = config.getString("Messages.Misc.ConsoleSilentSwap");
        this.consoleSilentJoin = config.getString("Messages.Misc.ConsoleSilentJoin");
        this.consoleSilentLeave = config.getString("Messages.Misc.ConsoleSilentLeave");


        /// Settings

        this.silentJoinDefaultState = config.getBoolean("Settings.SilentJoinDefaultState");

        this.swapServerMessageEnabled = config.getBoolean("Settings.SwapServerMessageEnabled");
        this.firstJoinNetworkMessageEnabled = config.getBoolean("Settings.FirstJoinNetworkMessageEnabled");
        this.joinNetworkMessageEnabled = config.getBoolean("Settings.JoinNetworkMessageEnabled");
        this.leaveNetworkMessageEnabled = config.getBoolean("Settings.LeaveNetworkMessageEnabled");

        this.notifyAdminsOnSilentMove = config.getBoolean("Settings.NotifyAdminsOnSilentMove");

        this.swapViewableByJoined = config.getBoolean("Settings.SwapServerMessageViewableBy.ServerJoined");
        this.swapViewableByLeft = config.getBoolean("Settings.SwapServerMessageViewableBy.ServerLeft");
        this.swapViewableByOther = config.getBoolean("Settings.SwapServerMessageViewableBy.OtherServer");

        this.firstJoinViewableByJoined = config.getBoolean("Settings.FirstJoinNetworkMessageViewableBy.ServerJoined");
        this.firstJoinViewableByOther = config.getBoolean("Settings.FirstJoinNetworkMessageViewableBy.OtherServer");

        this.joinViewableByJoined = config.getBoolean("Settings.JoinNetworkMessageViewableBy.ServerJoined");
        this.joinViewableByOther = config.getBoolean("Settings.JoinNetworkMessageViewableBy.OtherServer");

        this.leaveViewableByLeft = config.getBoolean("Settings.LeaveNetworkMessageViewableBy.ServerLeft");
        this.leaveViewableByOther = config.getBoolean("Settings.LeaveNetworkMessageViewableBy.OtherServer");

        // Blacklist
        this.blacklistedServers = config.getStringList("Settings.ServerBlacklist");
        this.useBlacklistAsWhitelist = config.getBoolean("Settings.UseBlacklistAsWhitelist");
        this.swapServerMessageRequires = config.getString("Settings.SwapServerMessageRequires").toUpperCase();

        this.serverFirstJoinMessageDisabled = config.getStringList("Settings.IgnoreFirstJoinMessagesList");
        this.serverJoinMessageDisabled = config.getStringList("Settings.IgnoreJoinMessagesList");
        this.serverLeaveMessageDisabled = config.getStringList("Settings.IgnoreLeaveMessagesList");

        this.treatVanishedPlayersAsSilent = config.getBoolean("OtherPlugins.PremiumVanish.TreatVanishedPlayersAsSilent");
        this.removeVanishedPlayersFromPlayerCount = config.getBoolean("OtherPlugins.PremiumVanish.RemoveVanishedPlayersFromPlayerCount");

        this.shouldSuppressLimboSwap = config.getBoolean("OtherPlugins.LimboAPI.SuppressSwapMessages");
        this.shouldSuppressLimboJoin = config.getBoolean("OtherPlugins.LimboAPI.SuppressJoinMessages");
        this.shouldSuppressLimboLeave = config.getBoolean("OtherPlugins.LimboAPI.SuppressLeaveMessages");

        this.debug = config.getBoolean("debug");

        // Verify Swap Server Message
        switch (swapServerMessageRequires) {
            case "JOINED":
            case "LEFT":
            case "BOTH":
            case "ANY":
                break;
            default:
                plugin.getCoreLogger()
                    .info(
                        "Setting error: Settings.SwapServerMessageRequires " +
                            "only allows JOINED LEFT BOTH or ANY. Got " +
                            swapServerMessageRequires +
                            "Defaulting to ANY."
                    );
                this.swapServerMessageRequires = "ANY";
        }
    }

    public String getSilentPrefix() {
        return silentPrefix;
    }

    public String getConsoleSilentSwap() {
        return consoleSilentSwap;
    }

    public String getConsoleSilentJoin() {
        return consoleSilentJoin;
    }

    public String getConsoleSilentLeave() {
        return consoleSilentLeave;
    }

    public boolean getSilentMessageState(CorePlayer player) {
        if (!player.hasPermission("networkjoinmessages.silent")) {
            return false;
        }

        if (messageState.containsKey(player.getUniqueId())) {
            return messageState.get(player.getUniqueId());
        } else {
            messageState.put(player.getUniqueId(), silentJoinDefaultState);
            return silentJoinDefaultState;
        }
    }
    public void setSilentMessageState(CorePlayer player, boolean state) {
        messageState.put(player.getUniqueId(), state);
    }

    public boolean isConnected(CorePlayer player) {
        return onlinePlayers.contains(player.getUniqueId());
    }
    public void setConnected(CorePlayer player, boolean state) {
        if (state == isConnected(player)) {
            return;
        }

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

    private void setJoinState(UUID id, boolean state) {
        if (state) {
            noJoinMessage.remove(id);
        } else {
            if (!noJoinMessage.contains(id)) {
                noJoinMessage.add(id);
            }
        }
    }
    private void setLeaveState(UUID id, boolean state) {
        if (state) {
            noLeaveMessage.remove(id);
        } else {
            if (!noLeaveMessage.contains(id)) {
                noLeaveMessage.add(id);
            }
        }
    }
    private void setSwapState(UUID id, boolean state) {
        if (state) {
            noSwitchMessage.remove(id);
        } else {
            if (!noSwitchMessage.contains(id)) {
                noSwitchMessage.add(id);
            }
        }
    }

    public void setSendMessageState(String list, UUID id, boolean state) {
        switch (list) {
            case "all":
                setSwapState(id, state);
                setJoinState(id, state);
                setLeaveState(id, state);
                return;
            case "join":
                setJoinState(id, state);
                return;
            case "leave":
                setLeaveState(id, state);
                return;
            case "swap":
                setSwapState(id, state);
        }
    }

    public List<UUID> getIgnorePlayers(MessageType type) {
        return switch (type) {
            case JOIN, FIRST_JOIN -> noJoinMessage;
            case SWAP -> noSwitchMessage;
            case LEAVE -> noLeaveMessage;
        };
    }

    /**
     * Determines which players should receive a message based on the configured
     * visibility flags and the involved servers. This method encapsulates the
     * common logic shared by swap, first-join, join, and leave message handling.
     *
     * <p>The selection rules are as follows:
     * <ul>
     *   <li>If all relevant flags are {@code true}, all players are returned.</li>
     *   <li>If {@code viewableByOther} is {@code true}, all players are returned,
     *       but the groups corresponding to {@code false} flags are removed.</li>
     *   <li>If {@code viewableByOther} is {@code false}, only the groups with
     *       {@code true} flags are returned.</li>
     * </ul>
     *
     * @param viewableByJoined  whether players on the "joined" server should see the message
     * @param viewableByLeft    whether players on the "left" server should see the message
     *                          (ignored if {@code fromServer} is {@code null})
     * @param viewableByOther   whether players on other servers should see the message
     * @param toServer          the name of the "joined" server, or {@code null} if not applicable
     * @param fromServer        the name of the "left" server, or {@code null} if not applicable
     * @return a list of players who should receive the message, never {@code null}
     */
    private List<CorePlayer> getReceivers(
            boolean viewableByJoined,
            boolean viewableByLeft,
            boolean viewableByOther,
            String toServer,
            String fromServer
    ) {
        List<CorePlayer> receivers = new ArrayList<>();

        // Case 1: everyone
        if (viewableByJoined && (viewableByLeft || fromServer == null) && viewableByOther) {
            receivers.addAll(plugin.getAllPlayers());
            return receivers;
        }

        // Case 2: everyone except exclusions
        if (viewableByOther) {
            receivers.addAll(plugin.getAllPlayers());

            if (!viewableByJoined && toServer != null) {
                receivers.removeAll(getServerPlayers(toServer));
            }
            if (!viewableByLeft && fromServer != null) {
                receivers.removeAll(getServerPlayers(fromServer));
            }
            return receivers;
        }

        // Case 3: only specific servers
        if (viewableByJoined && toServer != null) {
            receivers.addAll(getServerPlayers(toServer));
        }
        if (viewableByLeft && fromServer != null) {
            receivers.addAll(getServerPlayers(fromServer));
        }

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
        if (backendServer == null) {
            return new ArrayList<>();
        }
        return backendServer.getPlayersConnected();
    }

    /**
     * Checks whether the given player is blocked from receiving a message
     * based on the current blacklist/whitelist configuration.
     *
     * @param player the player to check
     * @return {@code true} if the player is blocked, {@code false} otherwise
     */
    public boolean isBlacklisted(CorePlayer player) {
        String server = player.getCurrentServer().getName();
        boolean listed = blacklistedServers.contains(server);

        boolean result = useBlacklistAsWhitelist != listed;

        plugin.getCoreLogger().debug(String.format(
                "Blacklist check for player %s on server %s: listed=%s, mode=%s, result=%s",
                player.getName(), server, listed,
                useBlacklistAsWhitelist ? "WHITELIST" : "BLACKLIST",
                result
        ));

        return result;
    }

    /**
     * Checks whether a message involving two servers should be blocked,
     * based on the current blacklist/whitelist configuration and the
     * {@code swapServerMessageRequires} setting.
     *
     * @param from the "left" server name
     * @param to   the "joined" server name
     * @return {@code true} if the message is blocked, {@code false} otherwise
     */
    public boolean isBlacklisted(@Nullable String from, @Nullable String to) {
        boolean fromListed = from != null && blacklistedServers.contains(from);
        boolean toListed   = to   != null && blacklistedServers.contains(to);

        boolean result = switch (swapServerMessageRequires.toUpperCase()) {
            case "JOINED" -> toListed;
            case "LEFT" -> fromListed;
            case "ANY" -> fromListed || toListed;
            case "BOTH" -> fromListed && toListed;
            default -> {
                plugin.getCoreLogger().warn("Unrecognized swapServerMessageRequires value: "
                        + swapServerMessageRequires);
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
     * Collects the UUIDs of all players on servers where messages of the given
     * type are disabled. This is used to determine which players should be ignored
     * when sending messages.
     *
     * @param type the type of message being checked (FIRST_JOIN, JOIN, or LEAVE)
     *             (SWAP is not handled here and will always return an empty list)
     * @return a list of player UUIDs that should be ignored for the given message type
     */
    public List<UUID> getIgnoredServerPlayers(MessageType type) {
        List<String> disabledServers;
        switch (type) {
            case FIRST_JOIN:
                disabledServers = serverFirstJoinMessageDisabled;
                break;
            case JOIN:
                disabledServers = serverJoinMessageDisabled;
                break;
            case LEAVE:
                disabledServers = serverLeaveMessageDisabled;
                break;
            default:
                plugin.getCoreLogger().debug("No ignored servers defined for message type: " + type);
                return Collections.emptyList();
        }

        List<UUID> ignored = new ArrayList<>();
        for (String serverName : disabledServers) {
            CoreBackendServer backendServer = plugin.getServer(serverName);
            if (backendServer != null) {
                for (CorePlayer player : backendServer.getPlayersConnected()) {
                    ignored.add(player.getUniqueId());
                }
            } else {
                plugin.getCoreLogger().debug("Ignored server not found or offline: " + serverName);
            }
        }
        return ignored;
    }



    private String getRandomMessage(List<String> messageList) {
        if (messageList.isEmpty()) {
            return "";
        } else if (messageList.size() == 1) {
            return messageList.get(0);
        }
        Random random = new Random();
        int randomIndex = random.nextInt(messageList.size());
        return messageList.get(randomIndex);
    }

    private String getMessage(String message, List<String> messageList) {
        if (!message.isEmpty()) {
            return message;
        }
        return getRandomMessage(messageList);
    }

    //region Getters

    public List<String> getServerNames() {
        return List.of(serverDisplayNames.keySet().toArray(new String[0]));
    }

    public String getServerDisplayName(String serverName) {
        return serverDisplayNames.getOrDefault(serverName, serverName);
    }

    public String getSwapServerMessage() {
        return getMessage(swapServerMessage, swapMessages);
    }

    public String getFirstJoinNetworkMessage() {
        return getMessage(firstJoinNetworkMessage, firstJoinMessages);
    }

    public String getJoinNetworkMessage() {
        return getMessage(joinNetworkMessage, joinMessages);
    }

    public String getLeaveNetworkMessage() {
        return getMessage(leaveNetworkMessage, leaveMessages);
    }

    public boolean getNotifyAdminsOnSilentMove() {
        return notifyAdminsOnSilentMove;
    }

    public boolean isSwapServerMessageEnabled() {
        return swapServerMessageEnabled;
    }

    public boolean isFirstJoinNetworkMessageEnabled() {
        return firstJoinNetworkMessageEnabled;
    }

    public boolean isJoinNetworkMessageEnabled() {
        return joinNetworkMessageEnabled;
    }

    public boolean isLeaveNetworkMessageEnabled() {
        return leaveNetworkMessageEnabled;
    }

    public boolean getTreatVanishedPlayersAsSilent() {
        return treatVanishedPlayersAsSilent;
    }

    public boolean getRemoveVanishedPlayersFromPlayerCount() {
        return removeVanishedPlayersFromPlayerCount;
    }

    public boolean getShouldSuppressLimboSwap() {
        return shouldSuppressLimboSwap;
    }

    public boolean getShouldSuppressLimboJoin() {
        return shouldSuppressLimboJoin;
    }

    public boolean getShouldSuppressLimboLeave() {
        return shouldSuppressLimboLeave;
    }

    public boolean getDebug() {
        return debug;
    }

    public Collection<CustomChart> getCustomCharts() {
        List<CustomChart> customCharts = new ArrayList<>();

        customCharts.add(new SimplePie("swap_enabled", () -> String.valueOf(swapServerMessageEnabled)));
        customCharts.add(new SimplePie("first_join_enabled", () -> String.valueOf(firstJoinNetworkMessageEnabled)));
        customCharts.add(new SimplePie("join_enabled", () -> String.valueOf(joinNetworkMessageEnabled)));
        customCharts.add(new SimplePie("leave_enabled", () -> String.valueOf(leaveNetworkMessageEnabled)));

        customCharts.add(new SimplePie("random_swap", () -> String.valueOf(swapServerMessageEnabled && swapServerMessage.isEmpty())));
        customCharts.add(new SimplePie("random_first_join", () -> String.valueOf(firstJoinNetworkMessageEnabled && firstJoinNetworkMessage.isEmpty())));
        customCharts.add(new SimplePie("random_join", () -> String.valueOf(joinNetworkMessageEnabled && joinNetworkMessage.isEmpty())));
        customCharts.add(new SimplePie("random_leave", () -> String.valueOf(leaveNetworkMessageEnabled && leaveNetworkMessage.isEmpty())));

        customCharts.add(new SimplePie("silent_join_default_state", () -> String.valueOf(silentJoinDefaultState)));
        customCharts.add(new SimplePie("notify_admins_on_silent_move", () -> String.valueOf(notifyAdminsOnSilentMove)));

        customCharts.add(new SimplePie("swap_viewable_by", () -> {
            List<String> values = new ArrayList<>();

            if (swapViewableByJoined) values.add("joined");
            if (swapViewableByLeft)   values.add("left");
            if (swapViewableByOther)  values.add("other");

            if (values.size() == 3) return "all";
            if (values.isEmpty())   return "none";

            return String.join(" ", values);
        }));
        customCharts.add(new SimplePie("first_join_viewable_by", () -> {
            if (firstJoinViewableByJoined && firstJoinViewableByOther) return "all";
            if (firstJoinViewableByJoined) return "joined";
            if (firstJoinViewableByOther)  return "other";

            return "none";
        }));
        customCharts.add(new SimplePie("join_viewable_by", () -> {
            if (joinViewableByJoined && joinViewableByOther) return "all";
            if (joinViewableByJoined) return "joined";
            if (joinViewableByOther)  return "other";

            return "none";
        }));
        customCharts.add(new SimplePie("leave_viewable_by", () -> {
            if (leaveViewableByLeft && leaveViewableByOther) return "all";
            if (leaveViewableByLeft)  return "left";
            if (leaveViewableByOther) return "other";

            return "none";
        }));

        customCharts.add(new SimplePie("server_blacklist_is_default", () ->
            String.valueOf(blacklistedServers.equals(Objects.requireNonNull(ConfigManager.getPluginConfig().getDefaults()).getStringList("Settings.ServerBlacklist")))
        ));
        customCharts.add(new SimplePie("blacklist_is_whitelist", () -> String.valueOf(useBlacklistAsWhitelist)));

        customCharts.add(new SimplePie("swap_requires", () -> swapServerMessageRequires));

        customCharts.add(new SimplePie("ignore_first_join_list_is_default", () ->
            String.valueOf(serverFirstJoinMessageDisabled.equals(Objects.requireNonNull(ConfigManager.getPluginConfig().getDefaults()).getStringList("Settings.IgnoreFirstJoinMessagesList")))
        ));
        customCharts.add(new SimplePie("ignore_join_list_is_default", () ->
            String.valueOf(serverJoinMessageDisabled.equals(Objects.requireNonNull(ConfigManager.getPluginConfig().getDefaults()).getStringList("Settings.IgnoreJoinMessagesList")))
        ));
        customCharts.add(new SimplePie("ignore_leave_list_is_default", () ->
            String.valueOf(serverLeaveMessageDisabled.equals(Objects.requireNonNull(ConfigManager.getPluginConfig().getDefaults()).getStringList("Settings.IgnoreLeaveMessagesList")))
        ));

        // Other plugins
        customCharts.add(new SimplePie("premium_vanish_vanished_are_silent", () -> String.valueOf(treatVanishedPlayersAsSilent)));
        customCharts.add(new SimplePie("premium_vanish_remove_vanished_from_player_count", () -> String.valueOf(removeVanishedPlayersFromPlayerCount)));

        customCharts.add(new SimplePie("limbo_api_suppress_swap", () -> String.valueOf(shouldSuppressLimboSwap)));
        customCharts.add(new SimplePie("limbo_api_suppress_join", () -> String.valueOf(shouldSuppressLimboJoin)));
        customCharts.add(new SimplePie("limbo_api_suppress_leave", () -> String.valueOf(shouldSuppressLimboLeave)));

        // Present plugins
        customCharts.add(new SimplePie("premium_vanish_is_present", () -> String.valueOf(plugin.isPluginLoaded("PremiumVanish"))));
        customCharts.add(new SimplePie("limbo_api_is_present", () -> String.valueOf(plugin.isPluginLoaded("LimboAPI"))));
        customCharts.add(new SimplePie("papi_bridge_is_present", () -> String.valueOf(plugin.isPluginLoaded("PAPIProxyBridge"))));
        customCharts.add(new SimplePie("luck_perms_is_present", () -> String.valueOf(plugin.isPluginLoaded("LuckPerms"))));
        customCharts.add(new SimplePie("mini_placeholders_is_present", () -> String.valueOf(plugin.isPluginLoaded("MiniPlaceholders"))));

        // Discord integration
        customCharts.add(new SimplePie("discord_integration_enabled", () ->
            String.valueOf(ConfigManager.getDiscordConfig().getBoolean("Enabled"))
        ));

        return customCharts;
    }

    //endregion
}
