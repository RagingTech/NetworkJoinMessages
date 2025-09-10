package xyz.earthcow.networkjoinmessages.common;

import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.*;

/**
 * Singleton class handling the sending of messages to command senders
 */
public final class MessageHandler {

    private static MessageHandler instance;

    private final Storage storage = Storage.getInstance();
    private final Formatter formatter = Formatter.getInstance();

    // Prevent instantiation outside of class
    private MessageHandler() {}

    /**
     * Gets or creates the only instance of the MessageHandler class allowed to exist
     * @return The MessageHandler instance
     */
    public static MessageHandler getInstance() {
        if (instance == null) {
            instance = new MessageHandler();
        }
        return instance;
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
     * @param type - What type of message should be sent (switch/join/leave)
     * @param parseTarget - The player to fetch the server from and parse placeholders against
     */
    public void broadcastMessage(String text, MessageType type, CorePlayer parseTarget) {
        broadcastMessage(text, type, parseTarget, false);
    }

    /**
     * Send a message globally, with silent false
     * @param text - The text to be displayed
     * @param type - What type of message should be sent (switch/join/leave)
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
     * @param type - What type of message should be sent (switch/join/leave)
     * @param parseTarget - The player to fetch the server from and parse placeholders against
     * @param silent - Whether this message should be silent
     */
    public void broadcastMessage(String text, MessageType type, CorePlayer parseTarget, boolean silent) {
        broadcastMessage(text, type, parseTarget.getCurrentServer().getName(), "", parseTarget, silent);
    }

    /**
     * Send a message globally, with silent false
     * @param text - The text to be displayed
     * @param type - What type of message should be sent (switch/join/leave)
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
                receivers.addAll(storage.getSwitchMessageReceivers(to, from));
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
        sendMessage(Core.getInstance().getPlugin().getConsole(), text, parseTarget);

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

        if (!storage.getNotifyAdminsOnSilentMove()) {
            return;
        }

        for (CorePlayer p : Core.getInstance().getPlugin().getAllPlayers()) {
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
        sendMessage(Core.getInstance().getPlugin().getConsole(), message, parseTarget);
    }

    public String getServerPlayerCount(CorePlayer player, boolean leaving) {
        return getServerPlayerCount(player.getCurrentServer(), leaving, player);
    }

    public String getServerPlayerCount(String serverName, boolean leaving, CorePlayer player) {
        return getServerPlayerCount(Core.getInstance().getPlugin().getServer(serverName), leaving, player);
    }

    public String getServerPlayerCount(@NotNull CoreBackendServer backendServer, boolean leaving, CorePlayer player) {
        List<CorePlayer> players = backendServer.getPlayersConnected();

        PremiumVanish premiumVanish = Core.getInstance().getPlugin().getVanishAPI();

        if (premiumVanish != null && storage.getRemoveVanishedPlayersFromPlayerCount()) {
                List<UUID> vanishedPlayers = premiumVanish.getInvisiblePlayers();
                // Filter out vanished players
                players = players.stream().filter(corePlayer -> !vanishedPlayers.contains(corePlayer.getUniqueId())).toList();
        }

        int count = players.size();

        if (player != null && leaving) {
            if (players.stream().anyMatch(corePlayer -> corePlayer.getUniqueId().equals(player.getUniqueId()))) {
                count--;
            }
        }

        return count + "";
    }

    public String getNetworkPlayerCount(CorePlayer player, boolean leaving) {
        Collection<CorePlayer> players = Core.getInstance().getPlugin().getAllPlayers();
        int count = players.size();

        PremiumVanish premiumVanish = Core.getInstance().getPlugin().getVanishAPI();

        boolean vanished = false;
        if (premiumVanish != null && storage.getRemoveVanishedPlayersFromPlayerCount()) {
            count -= premiumVanish.getInvisiblePlayers().size();
            if (player != null) {
                vanished = premiumVanish.isVanished(player.getUniqueId());
            }
        }

        if (player != null && !vanished && leaving) {
            if (players.stream().map(CorePlayer::getUniqueId).toList().contains(player.getUniqueId())) {
                count--;
            }
        }
        return count + "";
    }

    public String parseSwitchMessage(CorePlayer player, String fromName, String toName) {
        String from = storage.getServerDisplayName(fromName);
        String to = storage.getServerDisplayName(toName);
        return storage.getSwapServerMessage()
            .replace("%to%", to)
            .replace("%to_clean%", toName)
            .replace("%from%", from)
            .replace("%from_clean%", fromName)
            .replace("%playercount_from%", getServerPlayerCount(fromName, true, player))
            .replace("%playercount_to%", getServerPlayerCount(toName, false, player))
            .replace("%playercount_network%", getNetworkPlayerCount(player, false));
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
        return message
                .replace("%playercount_server%", getServerPlayerCount(player, leaving))
                .replace("%playercount_network%", getNetworkPlayerCount(player, leaving));
    }

    public void log(String string) {
        Core.getInstance().getPlugin().getCoreLogger().info(string);
    }
}
