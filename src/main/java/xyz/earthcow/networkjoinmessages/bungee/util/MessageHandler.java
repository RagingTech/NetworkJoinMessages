package xyz.earthcow.networkjoinmessages.bungee.util;

import de.myzelyam.api.vanish.BungeeVanishAPI;
import java.util.*;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import xyz.earthcow.networkjoinmessages.bungee.general.BungeeMain;
import xyz.earthcow.networkjoinmessages.bungee.general.Storage;

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
        Configuration config = BungeeMain.getInstance().getConfig();
        SwapServerMessage = config.getString(
            "Messages.SwapServerMessage",
            "&6&l%player%&r  &7[%from%&7] -> [%to%&7]"
        );
        JoinNetworkMessage = config.getString(
            "Messages.JoinNetworkMessage",
            "&6%player% &6has connected to the network!"
        );
        LeaveNetworkMessage = config.getString(
            "Messages.LeaveNetworkMessage",
            "&6%player% &6has disconnected from the network!"
        );

        HashMap<String, String> serverNames = new HashMap<String, String>();

        for (String server : config.getSection("Servers").getKeys()) {
            //Main.getInstance().getLogger().info("Looping: " + server);
            serverNames.put(
                server.toLowerCase(),
                config.getString("Servers." + server, server)
            );
            //Main.getInstance().getLogger().info("Put: " + server.toLowerCase() + " as " + config.getString("Servers." + server, server));
        }

        this.serverNames = serverNames;
    }

    public String getServerName(String key) {
        String name = key;
        if (serverNames != null) {
            //Main.getInstance().getLogger().info("ServerNames is not null");
            if (serverNames.containsKey(key.toLowerCase())) {
                //Main.getInstance().getLogger().info("serverNames contains the key");
                name = serverNames.get(key.toLowerCase());
            }
        }
        //Main.getInstance().getLogger().info("Fetched " + key + " got " + name);
        return name;
    }

    /**
     * Send a message globally, based on the players current server.
     * @param text - The text to be displayed
     * @param type - What type of message should be sent (switch/join/leave)
     * @param p - The player to fetch the server from.
     */
    public void broadcastMessage(String text, String type, ProxiedPlayer p) {
        if (p.getServer() == null) {
            //Fixes NPE when connecting to offline server.
            MessageHandler.getInstance()
                .log(
                    "Broadcast Message of " +
                    p.getName() +
                    " halted as Server returned Null. #01"
                );
            return;
        }
        broadcastMessage(text, type, p.getServer().getInfo().getName(), "???");
    }

    public void broadcastMessage(
        String text,
        String type,
        String from,
        String to
    ) {
        TextComponent msg = new TextComponent();
        msg.setText(text);
        //You could also use a StringBuilder here to get the arguments.

        List<ProxiedPlayer> receivers = new ArrayList<ProxiedPlayer>();
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
            receivers.addAll(ProxyServer.getInstance().getPlayers());
        }

        //Remove the players that have messages disabled
        List<UUID> ignorePlayers = Storage.getInstance().getIgnorePlayers(type);
        BungeeMain.getInstance().getLogger().info(text);

        //Add the players that are on ignored servers to the ignored list.
        ignorePlayers.addAll(
            Storage.getInstance().getIgnoredServerPlayers(type)
        );

        //Parse through all receivers and ignore the ones that are on the ignore list.
        for (ProxiedPlayer player : receivers) {
            if (ignorePlayers.contains(player.getUniqueId())) {
                continue;
            } else {
                player.sendMessage(msg);
            }
        }
        //ProxyServer.getInstance().broadcast(msg);
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

    public Iterable<String> getServerNames() {
        if (serverNames != null) {
            return serverNames.keySet();
        }
        return null;
    }

    public String getServerPlayerCount(ProxiedPlayer player, boolean leaving) {
        return getServerPlayerCount(
            player.getServer().getInfo(),
            leaving,
            player
        );
    }

    public String getServerPlayerCount(
        String serverName,
        boolean leaving,
        ProxiedPlayer player
    ) {
        return getServerPlayerCount(
            BungeeMain.getInstance().getProxy().getServers().get(serverName),
            leaving,
            player
        );
    }

    public String getServerPlayerCount(
        ServerInfo serverInfo,
        boolean leaving,
        ProxiedPlayer player
    ) {
        String serverPlayerCount = "?";
        if (serverInfo != null) {
            int count = 0;
            List<ProxiedPlayer> players = new ArrayList<ProxiedPlayer>(
                serverInfo.getPlayers()
            );

            //VanishAPI Count
            if (BungeeMain.getInstance().VanishAPI) {
                if (
                    BungeeMain.getInstance()
                        .getConfig()
                        .getBoolean(
                            "OtherPlugins.PremiumVanish.RemoveVanishedPlayersFromPlayerCount",
                            true
                        )
                ) {
                    List<UUID> vanished = BungeeVanishAPI.getInvisiblePlayers();
                    for (ProxiedPlayer p : serverInfo.getPlayers()) {
                        if (vanished.contains(p.getUniqueId())) {
                            players.remove(p);
                        }
                    }
                }
            }
            if (leaving && player != null) {
                if (players.contains(player)) {
                    count = players.size() - 1;
                }
            } else if (player != null) {
                if (!players.contains(player)) {
                    count = players.size() + 1;
                } else {
                    count = players.size();
                }
            }
            if (count < 0) {
                count = 0;
            }

            serverPlayerCount = String.valueOf(count);
        }
        return serverPlayerCount;
    }

    public String getNetworkPlayerCount(ProxiedPlayer player, Boolean leaving) {
        Collection<ProxiedPlayer> players = BungeeMain.getInstance()
            .getProxy()
            .getPlayers();
        if (leaving && player != null) {
            if (players.contains(player)) {
                return String.valueOf(players.size() - 1);
            } else {
                return String.valueOf(players.size());
            }
        } else if (player != null) {
            if (players.contains(player)) {
                return String.valueOf(players.size());
            } else {
                return String.valueOf(players.size() + 1);
            }
        }
        return String.valueOf(players.size());
    }

    public String formatMessage(String msg, ProxiedPlayer player) {
        String serverName = getServerName(
            player.getServer().getInfo().getName()
        );
        String formattedMsg = msg
            .replace("%player%", player.getName())
            .replace("%displayname%", player.getDisplayName())
            .replace("%server_name%", serverName)
            .replace(
                "%server_name_clean%",
                ChatColor.stripColor(
                    ChatColor.translateAlternateColorCodes('&', serverName)
                )
            );
        if (BungeeMain.getInstance().LuckPermsAPI) {
            LuckPerms lp = LuckPermsProvider.get();
            User lpUser = lp.getUserManager().getUser(player.getUniqueId());
            if (lpUser != null) {
                String rank = lpUser.getPrimaryGroup();
                String displayRank = rank;
                Group group = lp.getGroupManager().getGroup(rank);
                if (group != null) {
                    displayRank = Objects.requireNonNullElse(
                        group.getDisplayName(),
                        rank
                    );
                }
                formattedMsg = formattedMsg
                    .replace(
                        "%lp_rank%",
                        ChatColor.translateAlternateColorCodes('&', rank)
                    )
                    .replace(
                        "%lp_rank_display%",
                        ChatColor.translateAlternateColorCodes('&', displayRank)
                    );
            }
        }
        return formattedMsg;
    }

    public String formatSwitchMessage(
        ProxiedPlayer player,
        String fromName,
        String toName
    ) {
        String from = getServerName(fromName);
        String to = getServerName(toName);
        return formatMessage(getSwapServerMessage(), player)
            .replace("%to%", to)
            .replace(
                "%to_clean%",
                ChatColor.stripColor(
                    ChatColor.translateAlternateColorCodes('&', to)
                )
            )
            .replace("%from%", from)
            .replace(
                "%from_clean%",
                ChatColor.stripColor(
                    ChatColor.translateAlternateColorCodes('&', from)
                )
            )
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

    public String formatJoinMessage(ProxiedPlayer player) {
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

    public String formatQuitMessage(ProxiedPlayer player) {
        return formatMessage(getLeaveNetworkMessage(), player)
            .replace("%playercount_server%", getServerPlayerCount(player, true))
            .replace(
                "%playercount_network%",
                getNetworkPlayerCount(player, true)
            );
    }

    public void log(String string) {
        BungeeMain.getInstance().getLogger().info(string);
    }
}
