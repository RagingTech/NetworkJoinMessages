package xyz.earthcow.networkjoinmessages.velocity.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.myzelyam.api.vanish.BungeeVanishAPI;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import xyz.earthcow.networkjoinmessages.velocity.general.Storage;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;

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
        CommentedConfigurationNode rootNode = VelocityMain.getInstance()
            .getRootNode();
        SwapServerMessage = rootNode
            .node("Messages", "SwapServerMessage")
            .getString("&6&l%player%&r  &7[%from%&7] -> [%to%&7]");
        JoinNetworkMessage = rootNode
            .node("Messages", "JoinNetworkMessage")
            .getString("&6%player% &6has connected to the network!");
        LeaveNetworkMessage = rootNode
            .node("Messages", "LeaveNetworkMessage")
            .getString("&6%player% &6has disconnected from the network!");

        HashMap<String, String> serverNames = new HashMap<String, String>();

        for (ConfigurationNode server : rootNode
            .node("Servers")
            .childrenList()) {
            //Main.getInstance().getLogger().info("Looping: " + server);
            serverNames.put(
                server.key() + "",
                rootNode
                    .node(server.key(), server.getString())
                    .getString(server.getString())
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
    public void broadcastMessage(String text, String type, Player p) {
        if (!p.getCurrentServer().isPresent()) {
            //Fixes NPE when connecting to offline server.
            MessageHandler.getInstance()
                .log(
                    "Broadcast Message of " +
                    p.getUsername() +
                    " halted as Server returned Null. #01"
                );
            return;
        }
        broadcastMessage(
            text,
            type,
            p.getCurrentServer().get().getServerInfo().getName(),
            "???"
        );
    }

    public void broadcastMessage(
        String text,
        String type,
        String from,
        String to
    ) {
        TextComponent msg = Component.text(text);

        List<Player> receivers = new ArrayList<Player>();
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
                VelocityMain.getInstance().getProxy().getAllPlayers()
            );
        }

        //Remove the players that have messages disabled
        List<UUID> ignorePlayers = Storage.getInstance().getIgnorePlayers(type);
        VelocityMain.getInstance().getLogger().info(text);

        //Add the players that are on ignored servers to the ignored list.
        ignorePlayers.addAll(
            Storage.getInstance().getIgnoredServerPlayers(type)
        );

        //Parse through all receivers and ignore the ones that are on the ignore list.
        for (Player player : receivers) {
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

    public List<String> getServerNames() {
        if (serverNames != null) {
            return List.of(serverNames.keySet().toArray(new String[0]));
        }
        return null;
    }

    public String getServerPlayerCount(Player player, boolean leaving) {
        if (player.getCurrentServer().isPresent()) {
            return getServerPlayerCount(
                Optional.of(player.getCurrentServer().get().getServer()),
                leaving,
                player
            );
        }
        return "?";
    }

    public String getServerPlayerCount(
        String serverName,
        boolean leaving,
        Player player
    ) {
        return getServerPlayerCount(
            VelocityMain.getInstance().getProxy().getServer(serverName),
            leaving,
            player
        );
    }

    public String getServerPlayerCount(
        Optional<RegisteredServer> registeredServer,
        boolean leaving,
        Player player
    ) {
        String serverPlayerCount = "?";
        if (registeredServer.isPresent()) {
            int count = 0;
            List<Player> players = new ArrayList<Player>(
                registeredServer.get().getPlayersConnected()
            );

            //VanishAPI Count
            if (VelocityMain.getInstance().VanishAPI) {
                if (
                    VelocityMain.getInstance()
                        .getRootNode()
                        .node(
                            "OtherPlugins",
                            "PremiumVanish",
                            "RemoveVanishedPlayersFromPlayerCount"
                        )
                        .getBoolean(true)
                ) {
                    List<UUID> vanished = BungeeVanishAPI.getInvisiblePlayers();
                    for (Player p : registeredServer
                        .get()
                        .getPlayersConnected()) {
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

    public String getNetworkPlayerCount(Player player, Boolean leaving) {
        Collection<Player> players = VelocityMain.getInstance()
            .getProxy()
            .getAllPlayers();
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

    public String formatMessage(String msg, Player player) {
        if (!player.getCurrentServer().isPresent()) {
            try {
                VelocityMain.getInstance()
                    .getLogger()
                    .warn(
                        "WAITING FOR 1 SECOND BECAUSE PLAYER SERVER NULL MH1"
                    );
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }
        String serverName = player.getCurrentServer().isPresent()
            ? getServerName(
                player.getCurrentServer().get().getServerInfo().getName()
            )
            : "???";
        String formattedMsg = msg
            .replace("%player%", player.getUsername())
            .replace("%displayname%", player.getUsername())
            .replace("%server_name%", serverName)
            .replace(
                "%server_name_clean%",
                PlainTextComponentSerializer.plainText()
                    .serialize(
                        LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(serverName)
                    )
            );
        if (VelocityMain.getInstance().LuckPermsAPI) {
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
                        LegacyComponentSerializer.legacySection()
                            .serialize(
                                LegacyComponentSerializer.legacyAmpersand()
                                    .deserialize(rank)
                            )
                    )
                    .replace(
                        "%lp_rank_display%",
                        LegacyComponentSerializer.legacySection()
                            .serialize(
                                LegacyComponentSerializer.legacyAmpersand()
                                    .deserialize(displayRank)
                            )
                    );
            }
        }
        return formattedMsg;
    }

    public String formatSwitchMessage(
        Player player,
        String fromName,
        String toName
    ) {
        String from = getServerName(fromName);
        String to = getServerName(toName);
        return formatMessage(getSwapServerMessage(), player)
            .replace("%to%", to)
            .replace(
                "%to_clean%",
                PlainTextComponentSerializer.plainText()
                    .serialize(
                        LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(to)
                    )
            )
            .replace("%from%", from)
            .replace(
                "%from_clean%",
                PlainTextComponentSerializer.plainText()
                    .serialize(
                        LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(from)
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

    public String formatJoinMessage(Player player) {
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

    public String formatQuitMessage(Player player) {
        return formatMessage(getLeaveNetworkMessage(), player)
            .replace("%playercount_server%", getServerPlayerCount(player, true))
            .replace(
                "%playercount_network%",
                getNetworkPlayerCount(player, true)
            );
    }

    public void log(String string) {
        VelocityMain.getInstance().getLogger().info(string);
    }
}
