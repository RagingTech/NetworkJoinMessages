package xyz.earthcow.networkjoinmessages.common.listeners;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkQuitEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.common.general.Storage;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;

import java.util.stream.Collectors;

public class CorePlayerListener {

    private Component getSilentPrefix() {
        return MessageHandler.getMiniMessage().deserialize(ConfigManager.getPluginConfig().getString("Messages.Misc.SilentPrefix"));
    }

    public void onPreConnect(CorePlayer player, String previousServerName) {
        if (player == null) {
            return;
        }

        if (previousServerName != null) {
            Storage.getInstance().setFrom(player, previousServerName);
        }
    }

    public void onServerConnected(@NotNull CorePlayer player, @NotNull CoreBackendServer server) {
        NetworkJoinMessagesCore.getInstance().getPlugin().runTaskAsync(() -> {
            if (!Storage.getInstance().isConnected(player)) {
                // If the player is NOT already connected they have just joined the network
                Storage.getInstance().setConnected(player, true);
                int count = 0;
                while (player.getCurrentServer() == null && count < 120) {
                    if (count % 5 == 0 && count != 0) {
                        NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().warn("Waiting for non-null server: waited for half a second " + count + " times");
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }
                    count++;
                }
                if (player.getCurrentServer() == null) {
                    NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger()
                            .severe(player.getName() + "'s server was still null after persistent checks! " +
                            "Please report this to the developer at https://github.com/RagingTech/NetworkJoinMessages/issues");
                    return;
                }
                player.setLastKnownConnectedServer(server);
                if (!Storage.getInstance().isJoinNetworkMessageEnabled()) {
                    return;
                }

                // Blacklist Check
                if (Storage.getInstance().blacklistCheck(player)) {
                    return;
                }

                // TODO Add vanish support

                Component message = MessageHandler.getInstance().formatJoinMessage(player);

                if (Storage.getInstance().getAdminMessageState(player)) {
                    // Silent
                    if (player.hasPermission("networkjoinmessages.fakemessage")) {
                        Component toggleNotif = MessageHandler.getMiniMessage().deserialize(
                            ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.JoinNotification")
                        );
                        player.sendMessage(toggleNotif);
                    }

                    // Send to console
                    NetworkJoinMessagesCore.getInstance().SilentEvent("JOIN", player.getName());

                    // Send to admin players
                    if (Storage.getInstance().notifyAdminsOnSilentMove()) {
                        for (CorePlayer p : NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()
                                .stream().filter(networkPlayer -> networkPlayer.hasPermission("networkjoinmessages.silent")).collect(Collectors.toList())) {
                            p.sendMessage(message.append(getSilentPrefix()));
                        }
                    }
                } else {
                    // Not silent
                    MessageHandler.getInstance().broadcastMessage(message, "join", player);
                }

                // All checks have passed to reach this point
                // Call the custom NetworkJoinEvent
                NetworkJoinEvent networkJoinEvent = new NetworkJoinEvent(
                        player,
                        MessageHandler.getInstance().getServerDisplayName(server.getName()),
                        Storage.getInstance().getAdminMessageState(player),
                        MessageHandler.extractPlainText(message)
                );
                NetworkJoinMessagesCore.getInstance().getDiscordWebhookIntegration().onNetworkJoin(networkJoinEvent);
                NetworkJoinMessagesCore.getInstance()
                        .getPlugin()
                        .fireEvent(networkJoinEvent);
                return;
            }
            player.setLastKnownConnectedServer(server);

            // If the player IS already connected they have just switched servers
            if (!Storage.getInstance().isElsewhere(player)) {
                return;
            }
            String to = server.getName();
            String from = Storage.getInstance().getFrom(player);

            if (!Storage.getInstance().isSwapServerMessageEnabled()) {
                return;
            }

            if (Storage.getInstance().blacklistCheck(from, to)) {
                return;
            }

            Component message = MessageHandler.getInstance()
                    .formatSwitchMessage(player, from, to);

            // Silent
            if (Storage.getInstance().getAdminMessageState(player)) {
                NetworkJoinMessagesCore.getInstance()
                        .SilentEvent("MOVE", player.getName(), from, to);
                if (Storage.getInstance().notifyAdminsOnSilentMove()) {
                    for (CorePlayer p : NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()) {
                        if (p.hasPermission("networkjoinmessages.silent")) {
                            p.sendMessage(message.append(getSilentPrefix()));
                        }
                    }
                }
            } else {
                MessageHandler.getInstance()
                        .broadcastMessage(message, "switch", from, to);
            }

            // Call the custom ServerSwapEvent
            SwapServerEvent swapServerEvent = new SwapServerEvent(
                    player,
                    MessageHandler.getInstance().getServerDisplayName(from),
                    MessageHandler.getInstance().getServerDisplayName(to),
                    Storage.getInstance().getAdminMessageState(player),
                    MessageHandler.extractPlainText(message)
            );
            NetworkJoinMessagesCore.getInstance().getDiscordWebhookIntegration().onSwapServer(swapServerEvent);
            NetworkJoinMessagesCore.getInstance()
                    .getPlugin().fireEvent(swapServerEvent);
        });
    }

    public void onDisconnect(CorePlayer player) {
        if (player == null) {
            return;
        }

        if (!Storage.getInstance().isConnected(player)) {
            player.setLastKnownConnectedServer(null);
            return;
        }

        Storage.getInstance().setConnected(player, false);

        if (!Storage.getInstance().isLeaveNetworkMessageEnabled()) {
            player.setLastKnownConnectedServer(null);
            return;
        }

        if (Storage.getInstance().blacklistCheck(player)) {
            player.setLastKnownConnectedServer(null);
            return;
        }

        Component message = MessageHandler.getInstance().formatQuitMessage(player);

        // Silent
        if (Storage.getInstance().getAdminMessageState(player)) {
            NetworkJoinMessagesCore.getInstance()
                .SilentEvent("QUIT", player.getName());
            if (Storage.getInstance().notifyAdminsOnSilentMove()) {
                for (CorePlayer p : NetworkJoinMessagesCore.getInstance().getPlugin().getAllPlayers()) {
                    if (p.hasPermission("networkjoinmessages.silent")) {
                        p.sendMessage(message.append(getSilentPrefix()));
                    }
                }
            }
        } else {
            MessageHandler.getInstance().broadcastMessage(message, "leave", player);
        }

        // Call the custom NetworkQuitEvent
        NetworkQuitEvent networkQuitEvent = new NetworkQuitEvent(
            player,
            MessageHandler.getInstance()
                .getServerDisplayName(
                    player.getCurrentServer() != null ? player.getCurrentServer().getName() : "???"
                ),
            Storage.getInstance().getAdminMessageState(player),
            MessageHandler.extractPlainText(message)
        );
        NetworkJoinMessagesCore.getInstance().getDiscordWebhookIntegration().onNetworkQuit(networkQuitEvent);
        NetworkJoinMessagesCore.getInstance()
            .getPlugin().fireEvent(networkQuitEvent);

        player.setLastKnownConnectedServer(null);
    }
}
