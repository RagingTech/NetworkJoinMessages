package xyz.earthcow.networkjoinmessages.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.modules.SayanVanishHook;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.*;

/**
 * Handles the sending of messages to command senders
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
        for (CorePlayer player : plugin.getAllPlayers()) {
            startLeaveCacheTaskForPlayer(player);
        }
    }

    public void startLeaveCacheTaskForPlayer(CorePlayer player) {
        if (storage.getLeaveCacheDuration() == 0) return;
        taskIds.put(
                player.getUniqueId(),
                plugin.runTaskRepeatedly(() -> updateCachedLeaveMessage(player), storage.getLeaveCacheDuration())
        );
    }

    public void stopLeaveCacheTaskForPlayer(CorePlayer player) {
        if (taskIds.isEmpty()) return;
        Integer taskId = taskIds.remove(player.getUniqueId());
        if (taskId == null) return;
        plugin.cancelTask(taskId);
    }

    public void updateCachedLeaveMessage(CorePlayer player) {
        plugin.getCoreLogger().debug("Updating cached leave message for player " + player.getName());
        formatter.parsePlaceholdersAndThen(formatLeaveMessage(player), player, player::setCachedLeaveMessage);
    }

    /**
     * Sends a message to the specified command sender.
     * If the command sender is a CorePlayer object
     * then it will be used to parse PAPI and MiniPlaceholders.
     * Designed for self invoked messages such as command responses.
     * @param sender The CoreCommandSender that will receive the message
     * @param message The message to be sent
     */
    public void sendMessage(CoreCommandSender sender, String message) {
        if (sender instanceof CorePlayer parseTarget) {
            sendMessage(sender, message, parseTarget);
        } else {
            sendMessage(sender, message, null);
        }
    }

    /**
     * Sends a message to the specified command sender.
     * Parses LuckPerms, PAPI, and MiniPlaceholders against the specified parse target
     * Replaces: %player%, %displayname%, %server_name%, %server_name_clean%
     * Designed for broadcast messages.
     * @param sender The CoreCommandSender that will receive the message
     * @param message The message to be sent
     * @param parseTarget The CorePlayer that will be the target for PAPI and MiniPlaceholders
     */
    public void sendMessage(CoreCommandSender sender, String message, CorePlayer parseTarget) {
        if (parseTarget != null) {
            formatter.parsePlaceholdersAndThen(message, parseTarget, formatted -> {
                sender.sendMessage(Formatter.deserialize(formatted));
            });
            return;
        }
        sender.sendMessage(Formatter.deserialize(message));
    }

    /**
     * Send a message globally, based on the players current server with silent false
     * @param text - The text to be displayed
     * @param type - What type of message should be sent (swap/join/leave)
     * @param parseTarget - The player to fetch the server from and parse placeholders against
     */
    public void broadcastMessage(String text, MessageType type, CorePlayer parseTarget) {
        broadcastMessage(text, type, parseTarget, false);
    }

    /**
     * Send a message globally, with silent false
     * @param text - The text to be displayed
     * @param type - What type of message should be sent (swap/join/leave)
     * @param from - The server the player came from
     * @param to - The server the player went to
     * @param parseTarget - The player to parse placeholders against
     */
    public void broadcastMessage(String text, MessageType type, String from, String to, CorePlayer parseTarget) {
        broadcastMessage(text, type, from, to, parseTarget, false);
    }

    /**
     * Send a message globally, based on the players current server with silent false
     * @param text - The text to be displayed
     * @param type - What type of message should be sent (swap/join/leave)
     * @param parseTarget - The player to fetch the server from and parse placeholders against
     * @param silent - Whether this message should be silent
     */
    public void broadcastMessage(String text, MessageType type, CorePlayer parseTarget, boolean silent) {
        broadcastMessage(text, type, parseTarget.getCurrentServer().getName(), "", parseTarget, silent);
    }

    /**
     * Send a message globally, with silent false
     * @param text - The text to be displayed
     * @param type - What type of message should be sent (swap/join/leave)
     * @param from - The server the player came from
     * @param to - The server the player went to
     * @param parseTarget - The player to parse placeholders against
     * @param silent - Whether this message should be silent
     */
    public void broadcastMessage(String text, MessageType type, String from, String to, CorePlayer parseTarget, boolean silent) {
        if (silent) {
            broadcastSilentMessage(text, type, from, to, parseTarget);
            return;
        }

        List<CorePlayer> receivers = new ArrayList<>();

        switch (type) {
            case SWAP -> {
                receivers.addAll(storage.getSwapMessageReceivers(to, from));
            }
            case FIRST_JOIN -> {
                receivers.addAll(storage.getFirstJoinMessageReceivers(from));
            }
            case JOIN -> {
                receivers.addAll(storage.getJoinMessageReceivers(from));
            }
            case LEAVE -> {
                receivers.addAll(storage.getLeaveMessageReceivers(from));
            }
        }

        // Send message to console
        sendMessage(plugin.getConsole(), text, parseTarget);

        List<UUID> ignorePlayers = storage.getIgnorePlayers(type);
        ignorePlayers.addAll(storage.getIgnoredServerPlayers(type));

        for (CorePlayer player : receivers) {
            if (ignorePlayers.contains(player.getUniqueId())) {
                continue;
            }
            sendMessage(player, text, parseTarget);
        }
    }

    private void broadcastSilentMessage(@NotNull String text, @NotNull MessageType type, @NotNull String from, @NotNull String to, @NotNull CorePlayer parseTarget) {
        // Send message to console
        handleSilentConsoleMessage(type, from, to, parseTarget);

        if (!storage.isNotifyAdminsOnSilentMove()) {
            return;
        }

        for (CorePlayer p : plugin.getAllPlayers()) {
            if (p.hasPermission("networkjoinmessages.silent")) {
                sendMessage(p, storage.getSilentPrefix() + text, parseTarget);
            }
        }
    }

    private void handleSilentConsoleMessage(MessageType type, String from, String to, CorePlayer parseTarget) {
        String message = switch (type) {
            case SWAP -> storage.getConsoleSilentSwap()
                .replace("%to%", to)
                .replace("%from%", from);
            case FIRST_JOIN, JOIN -> storage.getConsoleSilentJoin();
            case LEAVE -> storage.getConsoleSilentLeave();
        };
        sendMessage(plugin.getConsole(), message, parseTarget);
    }

    public String getServerPlayerCount(CorePlayer player, boolean leaving) {
        return getServerPlayerCount(player.getCurrentServer(), leaving, player);
    }

    public String getServerPlayerCount(String serverName, boolean leaving, CorePlayer player) {
        return getServerPlayerCount(plugin.getServer(serverName), leaving, player);
    }

    private String getPlayerCount(Collection<CorePlayer> players, CorePlayer player, boolean leaving) {
        int count = players.size();
        PremiumVanish premiumVanish = plugin.getVanishAPI();
        boolean vanished = false;

        if (premiumVanish != null && storage.isPVRemoveVanishedPlayersFromPlayerCount()) {
            Set<UUID> vanishedPlayers = new HashSet<>(premiumVanish.getInvisiblePlayers());
            count -= vanishedPlayers.size();

            if (player != null) {
                vanished = vanishedPlayers.contains(player.getUniqueId());
            }

            // Rebuild player collection without vanished ones
            players = players.stream()
                .filter(p -> !vanishedPlayers.contains(p.getUniqueId()))
                .toList();
        }

        if (sayanVanishHook != null && storage.isSVRemoveVanishedPlayersFromPlayerCount()) {
            Collection<UUID> vanishedPlayers = sayanVanishHook.getVanishedPlayers();
            count -= vanishedPlayers.size();

            if (player != null) {
                vanished = vanishedPlayers.contains(player.getUniqueId());
            }

            // Rebuild player collection without vanished ones
            players = players.stream()
                .filter(p -> !vanishedPlayers.contains(p.getUniqueId()))
                .toList();
        }

        if (player != null && !vanished) {
            UUID playerId = player.getUniqueId();
            boolean isPresent = players.stream().anyMatch(p -> p.getUniqueId().equals(playerId));

            if (isPresent && leaving) {
                count--;
            } else if (!isPresent && !leaving) {
                count++;
            }
        }

        return Integer.toString(count);
    }

    public String getServerPlayerCount(@Nullable CoreBackendServer backendServer, boolean leaving, CorePlayer player) {
        if (backendServer == null) {
            return leaving ? "0" : "1";
        }
        return getPlayerCount(backendServer.getPlayersConnected(), player, leaving);
    }

    public String getNetworkPlayerCount(CorePlayer player, boolean leaving) {
        return getPlayerCount(plugin.getAllPlayers(), player, leaving);
    }

    public String parseSwapMessage(CorePlayer player, String fromName, String toName) {
        String from = storage.getServerDisplayName(fromName);
        String to = storage.getServerDisplayName(toName);

        String msg = storage.getSwapServerMessage()
            .replace("%to%", to)
            .replace("%to_clean%", toName)
            .replace("%from%", from)
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
        return formatLeaveDependentMessage(storage.getFirstJoinNetworkMessage(), player, false);
    }

    public String formatJoinMessage(CorePlayer player) {
        return formatLeaveDependentMessage(storage.getJoinNetworkMessage(), player, false);
    }

    public String formatLeaveMessage(CorePlayer player) {
        return formatLeaveDependentMessage(storage.getLeaveNetworkMessage(), player, true);
    }

    private String formatLeaveDependentMessage(String message, CorePlayer player, boolean leaving) {
        if (message.contains("%playercount_to%")) {
            message = message.replace("%playercount_server%", getServerPlayerCount(player, leaving));
        }
        if (message.contains("%playercount_network%")) {
            message = message.replace("%playercount_network%", getNetworkPlayerCount(player, leaving));
        }
        return message;
    }
}
