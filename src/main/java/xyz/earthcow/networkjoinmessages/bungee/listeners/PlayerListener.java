package xyz.earthcow.networkjoinmessages.bungee.listeners;

import de.myzelyam.api.vanish.BungeeVanishAPI;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.earthcow.networkjoinmessages.bungee.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.bungee.events.NetworkQuitEvent;
import xyz.earthcow.networkjoinmessages.bungee.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.bungee.general.BungeeMain;
import xyz.earthcow.networkjoinmessages.bungee.general.Storage;
import xyz.earthcow.networkjoinmessages.bungee.util.HexChat;
import xyz.earthcow.networkjoinmessages.bungee.util.MessageHandler;

public class PlayerListener implements Listener {

    String silent = BungeeMain.getInstance()
        .getConfig()
        .getString("Messages.Misc.SilentPrefix", "&7[Silent] ");

    @EventHandler
    public void prePlayerSwitchServer(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (player == null) {
            return;
        }

        if (e.getReason() != null) {
            if (
                e.getReason().equals(Reason.COMMAND) ||
                e.getReason().equals(Reason.JOIN_PROXY) ||
                e.getReason().equals(Reason.PLUGIN) ||
                e.getReason().equals(Reason.PLUGIN_MESSAGE)
            ) {
                //Normal connection reason. All is okay,
            } else {
                //Remove player from OldServer list, so that their movement is not notified.
                Storage.getInstance().clearPlayer(player);
            }
        }

        Server server = player.getServer();
        if (server != null) {
            String serverName = server.getInfo().getName();
            if (serverName != null) {
                Storage.getInstance()
                    .setFrom(player, server.getInfo().getName());
            }
        }
    }

    @EventHandler
    public void onPlayerSwitchServer(ServerConnectedEvent e) {
        ProxiedPlayer player = e.getPlayer();
        Server server = e.getServer();

        if (!Storage.getInstance().isConnected(player)) {
            return;
        }

        String to = server.getInfo().getName();
        String from = "???";
        if (Storage.getInstance().isElsewhere(player)) {
            from = Storage.getInstance().getFrom(player);
        } else {
            return; //Event was not a To-From event, so we send no message.
        }

        if (Storage.getInstance().isSwapServerMessageEnabled()) {
            if (Storage.getInstance().blacklistCheck(from, to)) {
                return;
            }

            String message = MessageHandler.getInstance()
                .formatSwitchMessage(player, from, to);

            //Silent
            if (Storage.getInstance().getAdminMessageState(player)) {
                BungeeMain.getInstance()
                    .SilentEvent("MOVE", player.getName(), from, to);
                if (Storage.getInstance().notifyAdminsOnSilentMove()) {
                    TextComponent silentMessage = new TextComponent(
                        HexChat.translateHexCodes(silent + message)
                    );
                    for (ProxiedPlayer p : BungeeMain.getInstance()
                        .getProxy()
                        .getPlayers()) {
                        if (p.hasPermission("networkjoinmessages.silent")) {
                            p.sendMessage(silentMessage);
                        }
                    }
                }
                //Not silent
            } else {
                //This one is special as there are certain settings in place.
                MessageHandler.getInstance()
                    .broadcastMessage(
                        HexChat.translateHexCodes(message),
                        "switch",
                        from,
                        to
                    );
            }

            // Call the custom ServerSwapEvent
            SwapServerEvent swapServerEvent = new SwapServerEvent(
                player,
                MessageHandler.getInstance().getServerName(from),
                MessageHandler.getInstance().getServerName(to),
                Storage.getInstance().getAdminMessageState(player),
                message
            );
            BungeeMain.getInstance()
                .getProxy()
                .getPluginManager()
                .callEvent(swapServerEvent);
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        ProxyServer.getInstance()
            .getScheduler()
            .schedule(
                BungeeMain.getInstance().getPlugin(),
                new Runnable() {
                    public void run() {
                        if (player.isConnected()) {
                            while (player.getServer() == null) {
                                try {
                                    BungeeMain.getInstance()
                                        .getLogger()
                                        .warning(
                                            player.getName() +
                                            "'s SERVER IS NULL WAITING A SECOND!!"
                                        );
                                    wait(1000);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            Storage.getInstance().setConnected(player, true);
                            if (
                                !Storage.getInstance()
                                    .isJoinNetworkMessageEnabled()
                            ) {
                                return;
                            }
                            String message = MessageHandler.getInstance()
                                .formatJoinMessage(player);

                            //VanishAPI support
                            if (BungeeMain.getInstance().VanishAPI) {
                                if (
                                    BungeeMain.getInstance()
                                        .getConfig()
                                        .getBoolean(
                                            "OtherPlugins.PremiumVanish.ToggleFakemessageWhenVanishing",
                                            false
                                        )
                                ) Storage.getInstance()
                                    .setAdminMessageState(
                                        player,
                                        BungeeVanishAPI.isInvisible(player)
                                    );
                            }

                            //Blacklist Check
                            if (Storage.getInstance().blacklistCheck(player)) {
                                return;
                            }

                            //Silent
                            if (
                                Storage.getInstance()
                                    .getAdminMessageState(player)
                            ) {
                                //Notify player about the toggle command.
                                if (
                                    player.hasPermission(
                                        "networkjoinmessages.fakemessage"
                                    )
                                ) {
                                    String toggleNotif =
                                        BungeeMain.getInstance()
                                            .getConfig()
                                            .getString(
                                                "Messages.Commands.Fakemessage.JoinNotification",
                                                "&7[BungeeJoin] You joined the server while silenced.\n" +
                                                "&7To have messages automatically enabled for you until\n" +
                                                "&7next reboot, use the command &f/fm toggle&7."
                                            );
                                    player.sendMessage(
                                        new TextComponent(
                                            HexChat.translateHexCodes(
                                                toggleNotif
                                            )
                                        )
                                    );
                                }

                                //Send to console
                                BungeeMain.getInstance()
                                    .SilentEvent("JOIN", player.getName());
                                //Send to admin players.
                                if (
                                    Storage.getInstance()
                                        .notifyAdminsOnSilentMove()
                                ) {
                                    TextComponent silentMessage =
                                        new TextComponent(
                                            HexChat.translateHexCodes(
                                                silent + message
                                            )
                                        );
                                    for (ProxiedPlayer p : BungeeMain.getInstance()
                                        .getProxy()
                                        .getPlayers()) {
                                        if (
                                            p.hasPermission(
                                                "networkjoinmessages.silent"
                                            )
                                        ) {
                                            p.sendMessage(silentMessage);
                                        }
                                    }
                                }
                                //Not silent
                            } else {
                                MessageHandler.getInstance()
                                    .broadcastMessage(
                                        HexChat.translateHexCodes(message),
                                        "join",
                                        player
                                    );
                            }

                            // All checks have passed to reach this point
                            // Call the custom NetworkJoinEvent
                            NetworkJoinEvent networkJoinEvent =
                                new NetworkJoinEvent(
                                    player,
                                    MessageHandler.getInstance()
                                        .getServerName(
                                            player
                                                .getServer()
                                                .getInfo()
                                                .getName()
                                        ),
                                    Storage.getInstance()
                                        .getAdminMessageState(player),
                                    message
                                );
                            BungeeMain.getInstance()
                                .getProxy()
                                .getPluginManager()
                                .callEvent(networkJoinEvent);
                        }
                    }
                },
                BungeeMain.getInstance()
                    .getConfig()
                    .getInt("Messages.Misc.JoinMessageDelaySeconds", 3),
                TimeUnit.SECONDS
            );
        //        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
        //
        //        }
    }

    @EventHandler
    public void onPostQuit(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        if (!Storage.getInstance().isConnected(player)) {
            return;
        }

        if (!Storage.getInstance().isLeaveNetworkMessageEnabled()) {
            Storage.getInstance().setConnected(player, false);
            return;
        }

        if (Storage.getInstance().blacklistCheck(player)) {
            return;
        }

        String message = MessageHandler.getInstance().formatQuitMessage(player);

        //Silent
        if (Storage.getInstance().getAdminMessageState(player)) {
            //Send to console
            BungeeMain.getInstance().SilentEvent("QUIT", player.getName());
            //Send to admin players.
            if (Storage.getInstance().notifyAdminsOnSilentMove()) {
                TextComponent silentMessage = new TextComponent(
                    HexChat.translateHexCodes(silent + message)
                );
                for (ProxiedPlayer p : BungeeMain.getInstance()
                    .getProxy()
                    .getPlayers()) {
                    if (p.hasPermission("networkjoinmessages.silent")) {
                        p.sendMessage(silentMessage);
                    }
                }
            }
            //Not silent
        } else {
            MessageHandler.getInstance()
                .broadcastMessage(
                    HexChat.translateHexCodes(message),
                    "leave",
                    player
                );
        }

        //Set them as not connected, as they have left the server.
        Storage.getInstance().setConnected(player, false);

        // Call the custom NetworkQuitEvent
        NetworkQuitEvent networkQuitEvent = new NetworkQuitEvent(
            player,
            MessageHandler.getInstance()
                .getServerName(player.getServer().getInfo().getName()),
            Storage.getInstance().getAdminMessageState(player),
            message
        );
        BungeeMain.getInstance()
            .getProxy()
            .getPluginManager()
            .callEvent(networkQuitEvent);
    }
}
