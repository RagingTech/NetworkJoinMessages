package xyz.earthcow.networkjoinmessages.common.util;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.common.general.Storage;

import java.util.*;
import java.util.stream.Collectors;

public class MessageHandler {

    private static MessageHandler instance;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    private LuckPerms luckPerms = null;

    public static MessageHandler getInstance() {
        if (instance == null) {
            instance = new MessageHandler();
        }
        return instance;
    }

    public static Component deserialize(String str) {
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(str);
        return miniMessage.deserialize(miniMessage.serialize(component));
    }

    public MessageHandler() {
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException | NoClassDefFoundError e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().warn("Could not find LuckPerms. Corresponding placeholders will be unavailable.");
        }
    }

    /**
     * Strips all MiniMessage tags and & color codes from the input string
     * and returns the plain text.
     *
     * @param input The input string containing MiniMessage syntax and/or & color codes.
     * @return The plain text without any formatting.
     */
    public static String stripTags(String input) {
        // Parse the input string into a Component
        Component component = miniMessage.deserialize(input);

        // Extract the plain text content from the Component
        return extractPlainText(component);
    }

    /**
     * Recursively extracts plain text from a Component.
     *
     * @param component The Component to extract text from.
     * @return The plain text content.
     */
    public static String extractPlainText(Component component) {
        if (component instanceof TextComponent) {
            return ((TextComponent) component).content();
        }

        // Recursively extract text from children
        StringBuilder builder = new StringBuilder();
        for (Component child : component.children()) {
            builder.append(extractPlainText(child));
        }
        return builder.toString();
    }

    String SwapServerMessage = "";
    String JoinNetworkMessage = "";
    String LeaveNetworkMessage = "";
    HashMap<String, String> serverNames;

    //String FirstTimeJoinMessage = "";

    public void setupConfigMessages() {
        YamlDocument config = ConfigManager.getPluginConfig();
        SwapServerMessage = config.getString("Messages.SwapServerMessage");
        JoinNetworkMessage = config.getString("Messages.JoinNetworkMessage");
        LeaveNetworkMessage = config.getString("Messages.LeaveNetworkMessage");

        HashMap<String, String> serverNames = new HashMap<String, String>();

        for (String serverKey : config.getSection("Servers").getRoutesAsStrings(false)) {
            serverNames.put(
                    serverKey.toLowerCase(),
                    config.getString("Servers." + serverKey, serverKey)
            );
        }

        this.serverNames = serverNames;
    }

    public String getServerDisplayName(String serverName) {
        String name = serverName;
        if (serverNames != null) {
            if (serverNames.containsKey(serverName.toLowerCase())) {
                name = serverNames.get(serverName.toLowerCase());
            }
        }
        return name;
    }

    /**
     * Send a message globally, based on the players current server.
     * @param text - The text to be displayed
     * @param type - What type of message should be sent (switch/join/leave)
     * @param player - The player to fetch the server from.
     */
    public void broadcastMessage(Component text, String type, CorePlayer player) {
        if (player.getCurrentServer() == null) {
            MessageHandler.getInstance().log("Broadcast Message of " + player.getName() + " halted as Server returned Null. #01");
            return;
        }
        broadcastMessage(text, type, player.getCurrentServer() == null ? "???" : player.getCurrentServer().getName(), "???");
    }

    public void broadcastMessage(Component text, String type, String from, String to) {
        List<CorePlayer> receivers = new ArrayList<>();
        if (type.equalsIgnoreCase("switch")) {
            receivers.addAll(Storage.getInstance().getSwitchMessageReceivers(to, from));
        } else if (type.equalsIgnoreCase("join")) {
            receivers.addAll(Storage.getInstance().getJoinMessageReceivers(from));
        } else if (type.equalsIgnoreCase("leave")) {
            receivers.addAll(Storage.getInstance().getLeaveMessageReceivers(from));
        } else {
            receivers.addAll(NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers());
        }

        List<UUID> ignorePlayers = Storage.getInstance().getIgnorePlayers(type);
        NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().info(extractPlainText(text));

        ignorePlayers.addAll(Storage.getInstance().getIgnoredServerPlayers(type));

        for (CorePlayer player : receivers) {
            if (ignorePlayers.contains(player.getUniqueId())) {
                continue;
            }
            player.sendMessage(text);
        }
    }

    public String getJoinNetworkMessage() {
        return JoinNetworkMessage;
    }

    public String getLeaveNetworkMessage() {
        return LeaveNetworkMessage;
    }

    public String getSwapServerMessage() {
        return SwapServerMessage;
    }

    public List<String> getServerNames() {
        if (serverNames != null) {
            return List.of(serverNames.keySet().toArray(new String[0]));
        }
        return null;
    }

    public String getServerPlayerCount(CorePlayer player, boolean leaving) {
        if (player.getCurrentServer() != null) {
            return getServerPlayerCount(
                player.getCurrentServer(),
                leaving,
                player
            );
        }
        return "?";
    }

    public String getServerPlayerCount(
        String serverName,
        boolean leaving,
        CorePlayer player
    ) {
        return getServerPlayerCount(
            NetworkJoinMessagesCore.getInstance().getPlugin().getServer(serverName),
            leaving,
            player
        );
    }

    public String getServerPlayerCount(
        CoreBackendServer backendServer,
        boolean leaving,
        CorePlayer player
    ) {
        String serverPlayerCount = "?";
        if (backendServer != null) {
            List<CorePlayer> players = new ArrayList<>(backendServer.getPlayersConnected());
            int count = players.size();

            // TODO Add vanish support

            if (leaving && player != null) {
                if (players.stream().map(CorePlayer::getUniqueId).collect(Collectors.toList()).contains(player.getUniqueId())) {
                    count--;
                }
            }

            serverPlayerCount = count + "";
        }
        return serverPlayerCount;
    }

    public String getNetworkPlayerCount(CorePlayer player, Boolean leaving) {
        Collection<CorePlayer> players = NetworkJoinMessagesCore.getInstance()
            .getPlugin()
            .getAllPlayers();
        int count = players.size();
        if (leaving && player != null) {
            if (players.stream().map(CorePlayer::getUniqueId).collect(Collectors.toList()).contains(player.getUniqueId())) {
                count--;
            }
        }
        return count + "";
    }

    public String handleLpPlaceholders(String str, CorePlayer player) {
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

    public Component formatMessage(String msg, CorePlayer player) {
        String serverName = player.getCurrentServer() != null
                ? getServerDisplayName(player.getCurrentServer().getName())
                : "???";
        String formattedMsg = handleLpPlaceholders(msg, player)
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getName())
                .replace("%server_name%", serverName)
                .replace("%server_name_clean%", stripTags(serverName));
        return deserialize(formattedMsg);
    }

    public Component formatSwitchMessage(CorePlayer player, String fromName, String toName) {
        String from = getServerDisplayName(fromName);
        String to = getServerDisplayName(toName);
        return formatMessage(
                getSwapServerMessage()
                    .replace("%to%", to)
                    .replace("%to_clean%", stripTags(to))
                    .replace("%from%", from)
                    .replace("%from_clean%", stripTags(from))
                    .replace("%playercount_from%", getServerPlayerCount(fromName, true, player))
                    .replace("%playercount_to%", getServerPlayerCount(toName, false, player))
                    .replace("%playercount_network%", getNetworkPlayerCount(player, false))
                , player);
    }

    public Component formatJoinMessage(CorePlayer player) {
        return formatMessage(
                getJoinNetworkMessage()
                    .replace("%playercount_server%", getServerPlayerCount(player, false))
                    .replace("%playercount_network%", getNetworkPlayerCount(player, false))
                , player);
    }

    public Component formatQuitMessage(CorePlayer player) {
        return formatMessage(
                getJoinNetworkMessage()
                        .replace("%playercount_server%", getServerPlayerCount(player, true))
                        .replace("%playercount_network%", getNetworkPlayerCount(player, true))
                , player);
    }

    public void log(String string) {
        NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().info(string);
    }
}
