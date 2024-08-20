package xyz.earthcow.networkjoinmessages.common.util;

import dev.dejvokep.boostedyaml.YamlDocument;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.common.general.Storage;

import java.util.*;
import java.util.stream.Collectors;

public class MessageHandler {

    private static MessageHandler instance;

    public static MessageHandler getInstance() {
        if (instance == null) {
            instance = new MessageHandler();
        }
        return instance;
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
    public void broadcastMessage(String text, String type, CorePlayer player) {
        if (player.getCurrentServer() == null) {
            //Fixes NPE when connecting to offline server.
            MessageHandler.getInstance()
                .log(
                    "Broadcast Message of " +
                    player.getName() +
                    " halted as Server returned Null. #01"
                );
            //return;
        }
        broadcastMessage(
            text,
            type,
            player.getCurrentServer() == null ? "???" : player.getCurrentServer().getName(),
            "???"
        );
    }

    public void broadcastMessage(String text, String type, String from, String to) {
        List<CorePlayer> receivers = new ArrayList<>();
        if (type.equalsIgnoreCase("switch")) {
            receivers.addAll(
                Storage.getInstance().getSwitchMessageReceivers(to, from)
            );
        } else if (type.equalsIgnoreCase("join")) {
            receivers.addAll(
                Storage.getInstance().getJoinMessageReceivers(from)
            );
        } else if (type.equalsIgnoreCase("leave")) {
            receivers.addAll(
                Storage.getInstance().getLeaveMessageReceivers(from)
            );
        } else {
            receivers.addAll(
                NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
            );
        }

        //Remove the players that have messages disabled
        List<UUID> ignorePlayers = Storage.getInstance().getIgnorePlayers(type);
        NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().info(text);

        //Add the players that are on ignored servers to the ignored list.
        ignorePlayers.addAll(
            Storage.getInstance().getIgnoredServerPlayers(type)
        );

        //Parse through all receivers and ignore the ones that are on the ignore list.
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

    public String formatMessage(String msg, CorePlayer player) {
        String serverName = player.getCurrentServer() != null
            ? getServerDisplayName(
                player.getCurrentServer().getName()
            )
            : "???";
        return msg
            .replace("%player%", player.getName())
            .replace("%displayname%", player.getName())
            .replace("%server_name%", serverName)
                // TODO Actually make this remove all formatting
            .replace("%server_name_clean%", serverName);
    }

    public String formatSwitchMessage(
        CorePlayer player,
        String fromName,
        String toName
    ) {
        String from = getServerDisplayName(fromName);
        String to = getServerDisplayName(toName);
        return formatMessage(getSwapServerMessage(), player)
            .replace("%to%", to)
                // TODO Actually make this remove all formatting
            .replace("%to_clean%", to)
            .replace("%from%", from)
                // TODO Actually make this remove all formatting
            .replace("%from_clean%", from)
            .replace(
                "%playercount_from%",
                getServerPlayerCount(fromName, true, player)
            )
            .replace(
                "%playercount_to%",
                getServerPlayerCount(toName, false, player)
            )
            .replace(
                "%playercount_network%",
                getNetworkPlayerCount(player, false)
            );
    }

    public String formatJoinMessage(CorePlayer player) {
        return formatMessage(getJoinNetworkMessage(), player)
            .replace(
                "%playercount_server%",
                getServerPlayerCount(player, false)
            )
            .replace(
                "%playercount_network%",
                getNetworkPlayerCount(player, false)
            );
    }

    public String formatQuitMessage(CorePlayer player) {
        return formatMessage(getLeaveNetworkMessage(), player)
            .replace("%playercount_server%", getServerPlayerCount(player, true))
            .replace(
                "%playercount_network%",
                getNetworkPlayerCount(player, true)
            );
    }

    public void log(String string) {
        NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().info(string);
    }
}
