package xyz.earthcow.networkjoinmessages.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.modules.SayanVanishHook;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.*;

/**
 * Handles formatting and dispatching messages to players and the console.
 */
public final class MessageHandler {

    private final CorePlugin plugin;
    private final Storage storage;
    private final Formatter formatter;
    private final Map<UUID, Integer> taskIds = new HashMap<>();

    @Nullable
    private final SayanVanishHook sayanVanishHook;

    public MessageHandler(CorePlugin plugin, Storage storage, Formatter formatter, @Nullable SayanVanishHook sayanVanishHook) {
        this.plugin = plugin;
        this.storage = storage;
        this.formatter = formatter;
        this.sayanVanishHook = sayanVanishHook;
        initCacheTasks();
    }

    public void initCacheTasks() {
        taskIds.values().forEach(plugin::cancelTask);
        taskIds.clear();
        if (storage.getLeaveCacheDuration() == 0) return;
        plugin.getAllPlayers().forEach(this::startLeaveCacheTaskForPlayer);
    }

    public void startLeaveCacheTaskForPlayer(CorePlayer player) {
        if (storage.getLeaveCacheDuration() == 0) return;
        taskIds.put(
            player.getUniqueId(),
            plugin.runTaskRepeatedly(() -> updateCachedLeaveMessage(player), storage.getLeaveCacheDuration())
        );
    }

    public void stopLeaveCacheTaskForPlayer(CorePlayer player) {
        Integer taskId = taskIds.remove(player.getUniqueId());
        if (taskId != null) plugin.cancelTask(taskId);
    }

    public void updateCachedLeaveMessage(CorePlayer player) {
        plugin.getCoreLogger().debug("Updating cached leave message for player " + player.getName());
        formatter.parsePlaceholdersAndThen(formatLeaveMessage(player), player, player::setCachedLeaveMessage);
    }

    /**
     * Sends a message to a command sender, using the sender as the placeholder parse target if they
     * are a player. Designed for self-directed messages such as command responses.
     *
     * @param sender  the recipient
     * @param message the message to send
     */
    public void sendMessage(CoreCommandSender sender, String message) {
        CorePlayer parseTarget = sender instanceof CorePlayer p ? p : null;
        sendMessage(sender, message, parseTarget);
    }

    /**
     * Sends a message to a command sender, parsing LuckPerms, PAPI, and MiniPlaceholders against
     * the given parse target. Designed for broadcast messages.
     *
     * @param sender      the recipient
     * @param message     the message to send
     * @param parseTarget the player to resolve placeholders against, or null to skip placeholder parsing
     */
    public void sendMessage(CoreCommandSender sender, String message, @Nullable CorePlayer parseTarget) {
        if (parseTarget != null) {
            formatter.parsePlaceholdersAndThen(message, parseTarget,
                formatted -> sender.sendMessage(Formatter.deserialize(formatted)));
        } else {
            sender.sendMessage(Formatter.deserialize(message));
        }
    }

    /**
     * Broadcasts a non-silent message, using the player's current server to determine receivers.
     */
    public void broadcastMessage(String text, MessageType type, CorePlayer parseTarget) {
        broadcastMessage(text, type, parseTarget, false);
    }

    /**
     * Broadcasts a message, using the player's current server as both the from/to context.
     */
    public void broadcastMessage(String text, MessageType type, CorePlayer parseTarget, boolean silent) {
        broadcastMessage(text, type, parseTarget.getCurrentServer().getName(), "", parseTarget, silent);
    }

    /**
     * Broadcasts a non-silent message with explicit from/to server context.
     */
    public void broadcastMessage(String text, MessageType type, String from, String to, CorePlayer parseTarget) {
        broadcastMessage(text, type, from, to, parseTarget, false);
    }

    /**
     * Broadcasts a message to all appropriate receivers. If silent, only notifies admins.
     *
     * @param text        the message text
     * @param type        the message type (swap/join/leave)
     * @param from        the origin server name
     * @param to          the destination server name
     * @param parseTarget the player to resolve placeholders against (may be null for leave messages)
     * @param silent      whether this is a silent (vanished) event
     */
    public void broadcastMessage(String text, MessageType type, String from, String to,
                                  @Nullable CorePlayer parseTarget, boolean silent) {
        if (silent) {
            broadcastSilentMessage(text, type, from, to, parseTarget);
            return;
        }

        List<CorePlayer> receivers = switch (type) {
            case SWAP       -> storage.getSwapMessageReceivers(to, from);
            case FIRST_JOIN -> storage.getFirstJoinMessageReceivers(from);
            case JOIN       -> storage.getJoinMessageReceivers(from);
            case LEAVE      -> storage.getLeaveMessageReceivers(from);
        };

        sendMessage(plugin.getConsole(), text, parseTarget);

        Set<UUID> ignorePlayers = new HashSet<>(storage.getIgnorePlayers(type));
        ignorePlayers.addAll(storage.getIgnoredServerPlayers(type));

        for (CorePlayer player : receivers) {
            if (!ignorePlayers.contains(player.getUniqueId())) {
                sendMessage(player, text, parseTarget);
            }
        }
    }

    private void broadcastSilentMessage(@NotNull String text, @NotNull MessageType type,
                                         @NotNull String from, @NotNull String to,
                                         @Nullable CorePlayer parseTarget) {
        handleSilentConsoleMessage(type, from, to, parseTarget);

        if (!storage.isNotifyAdminsOnSilentMove()) return;

        for (CorePlayer p : plugin.getAllPlayers()) {
            if (p.hasPermission("networkjoinmessages.silent")) {
                sendMessage(p, storage.getSilentPrefix() + text, parseTarget);
            }
        }
    }

    private void handleSilentConsoleMessage(MessageType type, String from, String to,
                                             @Nullable CorePlayer parseTarget) {
        String message = switch (type) {
            case SWAP            -> storage.getConsoleSilentSwap().replace("%to%", to).replace("%from%", from);
            case FIRST_JOIN, JOIN -> storage.getConsoleSilentJoin();
            case LEAVE           -> storage.getConsoleSilentLeave();
        };
        sendMessage(plugin.getConsole(), message, parseTarget);
    }

    public String getServerPlayerCount(CorePlayer player, boolean leaving) {
        return getServerPlayerCount(player.getCurrentServer(), leaving, player);
    }

    public String getServerPlayerCount(String serverName, boolean leaving, CorePlayer player) {
        return getServerPlayerCount(plugin.getServer(serverName), leaving, player);
    }

    public String getServerPlayerCount(@Nullable CoreBackendServer backendServer, boolean leaving, CorePlayer player) {
        if (backendServer == null) return leaving ? "0" : "1";
        return getPlayerCount(backendServer.getPlayersConnected(), player, leaving);
    }

    public String getNetworkPlayerCount(CorePlayer player, boolean leaving) {
        return getPlayerCount(plugin.getAllPlayers(), player, leaving);
    }

    private String getPlayerCount(Collection<CorePlayer> players, @Nullable CorePlayer player, boolean leaving) {
        // Build the effective vanished player set from enabled integrations
        Set<UUID> vanishedIds = new HashSet<>();

        PremiumVanish premiumVanish = plugin.getVanishAPI();
        if (premiumVanish != null && storage.isPVRemoveVanishedPlayersFromPlayerCount()) {
            vanishedIds.addAll(premiumVanish.getInvisiblePlayers());
        }
        if (sayanVanishHook != null && storage.isSVRemoveVanishedPlayersFromPlayerCount()) {
            vanishedIds.addAll(sayanVanishHook.getVanishedPlayers());
        }

        // Filter players to the visible set
        List<CorePlayer> visiblePlayers = players.stream()
            .filter(p -> !vanishedIds.contains(p.getUniqueId()))
            .toList();

        int count = visiblePlayers.size();

        if (player != null && !vanishedIds.contains(player.getUniqueId())) {
            UUID playerId = player.getUniqueId();
            boolean isPresent = visiblePlayers.stream().anyMatch(p -> p.getUniqueId().equals(playerId));
            if (isPresent && leaving)   count--;
            else if (!isPresent && !leaving) count++;
        }

        return Integer.toString(count);
    }

    public String parseSwapMessage(CorePlayer player, String fromName, String toName) {
        String from = storage.getServerDisplayName(fromName);
        String to   = storage.getServerDisplayName(toName);

        String msg = storage.getSwapServerMessage()
            .replace("%to%",         to)
            .replace("%to_clean%",   toName)
            .replace("%from%",       from)
            .replace("%from_clean%", fromName);

        if (msg.contains("%playercount_from%")) {
            msg = msg.replace("%playercount_from%", getServerPlayerCount(fromName, true, player));
        }
        if (msg.contains("%playercount_to%")) {
            msg = msg.replace("%playercount_to%", getServerPlayerCount(toName, false, player));
        }
        if (msg.contains("%playercount_network%")) {
            msg = msg.replace("%playercount_network%", getNetworkPlayerCount(player, false));
        }

        return msg;
    }

    public String formatFirstJoinMessage(CorePlayer player) {
        return formatPlayerCountMessage(storage.getFirstJoinNetworkMessage(), player, false);
    }

    public String formatJoinMessage(CorePlayer player) {
        return formatPlayerCountMessage(storage.getJoinNetworkMessage(), player, false);
    }

    public String formatLeaveMessage(CorePlayer player) {
        return formatPlayerCountMessage(storage.getLeaveNetworkMessage(), player, true);
    }

    /**
     * Replaces {@code %playercount_server%} and {@code %playercount_network%} in the given message.
     *
     * @param message the raw message template
     * @param player  the triggering player
     * @param leaving whether the player is leaving (affects count adjustment)
     * @return the message with player-count placeholders resolved
     */
    private String formatPlayerCountMessage(String message, CorePlayer player, boolean leaving) {
        if (message.contains("%playercount_server%")) {
            message = message.replace("%playercount_server%", getServerPlayerCount(player, leaving));
        }
        if (message.contains("%playercount_network%")) {
            message = message.replace("%playercount_network%", getNetworkPlayerCount(player, leaving));
        }
        return message;
    }
}
