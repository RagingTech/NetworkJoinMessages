package xyz.earthcow.networkjoinmessages.common.listeners;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkQuitEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.common.general.Storage;
import xyz.earthcow.networkjoinmessages.common.util.HexChat;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;

public class CorePlayerListener {

    private String getSilentPrefix() {
        return ConfigManager.getPluginConfig().getString("Messages.Misc.SilentPrefix");
    }

    public void onPreConnect(CorePlayer player, String previousServerName) {
        if (player == null) {
            return;
        }

        if (previousServerName != null) {
            Storage.getInstance().setFrom(player, previousServerName);
        }
    }

    public void onServerConnected(CorePlayer player, CoreBackendServer server) {
        if (!Storage.getInstance().isConnected(player)) {
            return;
        }

        String to = server.getName();
        String from = "???";
        if (Storage.getInstance().isElsewhere(player)) {
            from = Storage.getInstance().getFrom(player);
        } else {
            return;
        }

        if (Storage.getInstance().isSwapServerMessageEnabled()) {
            if (Storage.getInstance().blacklistCheck(from, to)) {
                return;
            }

            String message = MessageHandler.getInstance()
                .formatSwitchMessage(player, from, to);

            // Silent
            if (Storage.getInstance().getAdminMessageState(player)) {
                NetworkJoinMessagesCore.getInstance()
                    .SilentEvent("MOVE", player.getName(), from, to);
                if (Storage.getInstance().notifyAdminsOnSilentMove()) {
                    for (CorePlayer p : NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()) {
                        if (p.hasPermission("networkjoinmessages.silent")) {
                            p.sendMessage(HexChat.translateHexCodes(getSilentPrefix() + message));
                        }
                    }
                }
            } else {
                MessageHandler.getInstance()
                    .broadcastMessage(HexChat.translateHexCodes(message), "switch", from, to);
            }

            // Call the custom ServerSwapEvent
            SwapServerEvent swapServerEvent = new SwapServerEvent(
                player,
                MessageHandler.getInstance().getServerDisplayName(from),
                MessageHandler.getInstance().getServerDisplayName(to),
                Storage.getInstance().getAdminMessageState(player),
                message
            );
            NetworkJoinMessagesCore.getInstance().getDiscordWebhookIntegration().onSwapServer(swapServerEvent);
            NetworkJoinMessagesCore.getInstance()
                .getPlugin().fireEvent(swapServerEvent);
        }
    }

    public void onLogin(CorePlayer player) {
        if (player == null) {
            return;
        }

        NetworkJoinMessagesCore.getInstance()
            .getPlugin().runTaskLater(() -> {
                    while (player.getCurrentServer() == null) {
                        try {
                            NetworkJoinMessagesCore.getInstance().getPlugin()
                                .getCoreLogger()
                                .warn(
                                    player.getName() +
                                    "'s SERVER IS NULL WAITING A SECOND!!"
                                );
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    Storage.getInstance().setConnected(player, true);
                    if (!Storage.getInstance().isJoinNetworkMessageEnabled()) {
                        return;
                    }
                    String message = MessageHandler.getInstance()
                        .formatJoinMessage(player);

                    // TODO Add vanish support

                    // Blacklist Check
                    if (Storage.getInstance().blacklistCheck(player)) {
                        return;
                    }

                    // Silent
                    if (Storage.getInstance().getAdminMessageState(player)) {
                        if (player.hasPermission("networkjoinmessages.fakemessage")) {
                            String toggleNotif = ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.JoinNotification");
                            player.sendMessage(HexChat.translateHexCodes(toggleNotif));
                        }

                        // Send to console
                        NetworkJoinMessagesCore.getInstance()
                            .SilentEvent("JOIN", player.getName());
                        // Send to admin players
                        if (Storage.getInstance().notifyAdminsOnSilentMove()) {
                            for (CorePlayer p : NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()) {
                                if (p.hasPermission("networkjoinmessages.silent")) {
                                    p.sendMessage(HexChat.translateHexCodes(getSilentPrefix() + message));
                                }
                            }
                        }
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
                    NetworkJoinEvent networkJoinEvent = new NetworkJoinEvent(
                        player,
                        MessageHandler.getInstance().getServerDisplayName(player.getCurrentServer().getName()),
                        Storage.getInstance().getAdminMessageState(player),
                        message
                    );
                    NetworkJoinMessagesCore.getInstance().getDiscordWebhookIntegration().onNetworkJoin(networkJoinEvent);
                    NetworkJoinMessagesCore.getInstance()
                        .getPlugin()
                        .fireEvent(networkJoinEvent);
            }, ConfigManager.getPluginConfig().getInt("Messages.Misc.JoinMessageDelaySeconds"));
    }

    public void onDisconnect(CorePlayer player) {
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

        // Silent
        if (Storage.getInstance().getAdminMessageState(player)) {
            NetworkJoinMessagesCore.getInstance()
                .SilentEvent("QUIT", player.getName());
            if (Storage.getInstance().notifyAdminsOnSilentMove()) {
                for (CorePlayer p : NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()) {
                    if (p.hasPermission("networkjoinmessages.silent")) {
                        p.sendMessage(HexChat.translateHexCodes(getSilentPrefix() + message));
                    }
                }
            }
        } else {
            MessageHandler.getInstance()
                .broadcastMessage(
                    HexChat.translateHexCodes(message),
                    "leave",
                    player
                );
        }

        Storage.getInstance().setConnected(player, false);

        // Call the custom NetworkQuitEvent
        NetworkQuitEvent networkQuitEvent = new NetworkQuitEvent(
            player,
            MessageHandler.getInstance()
                .getServerDisplayName(
                    player.getCurrentServer() != null ? player.getCurrentServer().getName() : "???"
                ),
            Storage.getInstance().getAdminMessageState(player),
            message
        );
        NetworkJoinMessagesCore.getInstance().getDiscordWebhookIntegration().onNetworkQuit(networkQuitEvent);
        NetworkJoinMessagesCore.getInstance()
            .getPlugin().fireEvent(networkQuitEvent);
    }
}
