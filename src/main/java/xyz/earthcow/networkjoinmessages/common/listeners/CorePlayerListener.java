package xyz.earthcow.networkjoinmessages.common.listeners;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkQuitEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.Core;
import xyz.earthcow.networkjoinmessages.common.general.Storage;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

public class CorePlayerListener {

    private final Core core = Core.getInstance();
    private final CorePlugin plugin = core.getPlugin();

    @Nullable
    private final PremiumVanish premiumVanish = plugin.getVanishAPI();

    private String getSilentPrefix() {
        return ConfigManager.getPluginConfig().getString("Messages.Misc.SilentPrefix");
    }

    private void handlePlayerJoin(@NotNull CorePlayer player, @NotNull CoreBackendServer server) {
        Storage.getInstance().setConnected(player, true);
        player.setLastKnownConnectedServer(server);

        boolean firstJoin = !core.getFirstJoinTracker().hasJoined(player.getUniqueId());

        if (!firstJoin && !Storage.getInstance().isJoinNetworkMessageEnabled()) {
            return;
        }

        if (firstJoin) {
            core.getFirstJoinTracker().markAsJoined(player.getUniqueId(), player.getName());
            if (!Storage.getInstance().isFirstJoinNetworkMessageEnabled()) {
                return;
            }
        }

        // Blacklist Check
        if (Storage.getInstance().blacklistCheck(player)) {
            return;
        }

        if (Storage.getInstance().getShouldSuppressLimboJoin() && player.isInLimbo()) {
            return;
        }

        String message = firstJoin ? MessageHandler.getInstance().formatFirstJoinMessage(player) : MessageHandler.getInstance().formatJoinMessage(player);

        if (Storage.getInstance().getSilentMessageState(player)) {
            // Silent
            if (player.hasPermission("networkjoinmessages.fakemessage")) {
                Formatter.getInstance().parsePlaceholdersAndThen(
                        ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.JoinNotification"),
                        player,
                        formattedMsg -> {
                            MessageHandler.getInstance().sendMessage(player, formattedMsg);
                        }
                );

            }

            // Send to console
            core.SilentEvent("JOIN", player.getName());

            // Send to admin players
            if (Storage.getInstance().getNotifyAdminsOnSilentMove()) {
                for (CorePlayer p : plugin.getAllPlayers()
                    .stream().filter(networkPlayer -> networkPlayer.hasPermission("networkjoinmessages.silent")).toList()) {
                    MessageHandler.getInstance().sendMessage(p, getSilentPrefix() + message, player);
                }
            }
        } else {
            // Not silent
            MessageHandler.getInstance().broadcastMessage(message, firstJoin ? MessageType.FIRST_JOIN : MessageType.JOIN, player);
        }

        Component formattedMessage = Formatter.deserialize(message);
        // All checks have passed to reach this point
        // Call the custom NetworkJoinEvent
        NetworkJoinEvent networkJoinEvent = new NetworkJoinEvent(
            player,
            Storage.getInstance().getServerDisplayName(server.getName()),
            Storage.getInstance().getSilentMessageState(player),
            firstJoin,
                Formatter.serialize(formattedMessage),
                Formatter.stripColor(formattedMessage)
        );
        core.getDiscordWebhookIntegration().onNetworkJoin(networkJoinEvent);
        plugin.fireEvent(networkJoinEvent);
    }

    private void handlePlayerSwap(@NotNull CorePlayer player, @NotNull CoreBackendServer server, boolean fromLimbo) {
        player.setLastKnownConnectedServer(server);

        if (Storage.getInstance().getShouldSuppressLimboSwap() && fromLimbo) {
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

        String message = MessageHandler.getInstance()
            .parseSwitchMessage(player, from, to);

        // Silent
        if (Storage.getInstance().getSilentMessageState(player)) {
            Core.getInstance()
                .SilentEvent("MOVE", player.getName(), from, to);
            if (Storage.getInstance().getNotifyAdminsOnSilentMove()) {
                for (CorePlayer p : plugin.getAllPlayers()) {
                    if (p.hasPermission("networkjoinmessages.silent")) {
                        MessageHandler.getInstance().sendMessage(p, getSilentPrefix() + message, player);
                    }
                }
            }
        } else {
            MessageHandler.getInstance()
                .broadcastMessage(message, MessageType.SWAP, from, to, player);
        }

        Component formattedMessage = Formatter.deserialize(message);
        // Call the custom ServerSwapEvent
        SwapServerEvent swapServerEvent = new SwapServerEvent(
            player,
            Storage.getInstance().getServerDisplayName(from),
            Storage.getInstance().getServerDisplayName(to),
            Storage.getInstance().getSilentMessageState(player),
                Formatter.serialize(formattedMessage),
                Formatter.stripColor(formattedMessage)
        );
        core.getDiscordWebhookIntegration().onSwapServer(swapServerEvent);
        plugin.fireEvent(swapServerEvent);
    }

    public void onPreConnect(CorePlayer player, String previousServerName) {
        if (player == null) {
            return;
        }

        if (previousServerName != null) {
            Storage.getInstance().setFrom(player, previousServerName);
        }
    }

    public void onServerConnected(@NotNull CorePlayer player, @NotNull CoreBackendServer server, @Nullable CoreBackendServer previousServer) {
        plugin.runTaskAsync(() -> {
            // PremiumVanish
            if (premiumVanish != null) {
                if (ConfigManager.getPluginConfig().getBoolean("Settings.OtherPlugins.PremiumVanish.ToggleFakemessageWhenVanishing")) {
                    Storage.getInstance().setSilentMessageState(player, premiumVanish.isVanished(player.getUniqueId()));
                }
            }

            if (!Storage.getInstance().isConnected(player)) {
                // If the player is NOT already connected they have just joined the network
                handlePlayerJoin(player, server);
                return;
            }
            // If the player IS already connected, then they have just switched servers

            // If the server type is Velocity and the previous server is null then the player MUST have come from a LimboAPI server
            boolean fromLimbo = plugin.getServerType().equals(ServerType.VELOCITY) && previousServer == null;
            handlePlayerSwap(player, server, fromLimbo);
        });
    }

    public void onDisconnect(CorePlayer player) {
        if (player == null) {
            return;
        }

        if (!Storage.getInstance().isConnected(player)) {
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
            return;
        }

        Storage.getInstance().setConnected(player, false);

        if (!Storage.getInstance().isLeaveNetworkMessageEnabled()) {
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
            return;
        }

        if (Storage.getInstance().blacklistCheck(player)) {
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
            return;
        }

        if (Storage.getInstance().getShouldSuppressLimboLeave() && player.isInLimbo()) {
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
            return;
        }

        // PremiumVanish
        if (premiumVanish != null) {
            if (ConfigManager.getPluginConfig().getBoolean("Settings.OtherPlugins.PremiumVanish.ToggleFakemessageWhenVanishing")) {
                Storage.getInstance().setSilentMessageState(player, premiumVanish.isVanished(player.getUniqueId()));
            }
        }

        String message = MessageHandler.getInstance().formatLeaveMessage(player);

        // Silent
        if (Storage.getInstance().getSilentMessageState(player)) {
            core.SilentEvent("QUIT", player.getName());
            if (Storage.getInstance().getNotifyAdminsOnSilentMove()) {
                for (CorePlayer p : plugin.getAllPlayers()) {
                    if (p.hasPermission("networkjoinmessages.silent")) {
                        MessageHandler.getInstance().sendMessage(p, getSilentPrefix() + message, player);
                    }
                }
            }
        } else {
            MessageHandler.getInstance().broadcastMessage(message, MessageType.LEAVE, player);
        }

        Component formattedMessage = Formatter.deserialize(message);
        // Call the custom NetworkQuitEvent
        NetworkQuitEvent networkQuitEvent = new NetworkQuitEvent(
            player,
            Storage.getInstance().getServerDisplayName(player.getCurrentServer().getName()),
            Storage.getInstance().getSilentMessageState(player),
                Formatter.serialize(formattedMessage),
                Formatter.stripColor(formattedMessage)
        );
        core.getDiscordWebhookIntegration().onNetworkQuit(networkQuitEvent);
        plugin.fireEvent(networkQuitEvent);

        plugin.getPlayerManager().removePlayer(player.getUniqueId());
    }
}
