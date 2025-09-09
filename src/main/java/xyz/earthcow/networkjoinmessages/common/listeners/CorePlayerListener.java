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
    private final Storage storage = Storage.getInstance();

    @Nullable
    private final PremiumVanish premiumVanish = plugin.getVanishAPI();

    /**
     * Helper function to determine if an event should or should not be silent
     * @param player Trigger player
     * @return True if the event is silent false otherwise
     */
    private boolean isSilentEvent(@NotNull CorePlayer player) {
        // Event is silent if, the player has a silent message state OR
        // premiumVanish is present, the treat vanished players as silent option is true, and the player is vanished
        return storage.getSilentMessageState(player) ||
                (premiumVanish != null && storage.getTreatVanishedPlayersAsSilent() && premiumVanish.isVanished(player.getUniqueId()));
    }

    /**
     * Handles a player joining the server
     * @param player Player that joined
     * @param server Server that the player joined
     */
    private void handlePlayerJoin(@NotNull CorePlayer player, @NotNull CoreBackendServer server) {
        storage.setConnected(player, true);
        player.setLastKnownConnectedServer(server);

        boolean firstJoin = !core.getFirstJoinTracker().hasJoined(player.getUniqueId());

        if (!firstJoin && !storage.isJoinNetworkMessageEnabled()) {
            return;
        }

        if (firstJoin) {
            core.getFirstJoinTracker().markAsJoined(player.getUniqueId(), player.getName());
            if (!storage.isFirstJoinNetworkMessageEnabled()) {
                return;
            }
        }

        // Blacklist Check
        if (storage.isBlacklisted(player)) {
            return;
        }

        if (storage.getShouldSuppressLimboJoin() && player.isInLimbo()) {
            return;
        }

        String message = firstJoin ? MessageHandler.getInstance().formatFirstJoinMessage(player) : MessageHandler.getInstance().formatJoinMessage(player);

        boolean isSilent = isSilentEvent(player);

        if (isSilent) {
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
            if (storage.getNotifyAdminsOnSilentMove()) {
                for (CorePlayer p : plugin.getAllPlayers()
                    .stream().filter(networkPlayer -> networkPlayer.hasPermission("networkjoinmessages.silent")).toList()) {
                    MessageHandler.getInstance().sendMessage(p, storage.getSilentPrefix() + message, player);
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
            storage.getServerDisplayName(server.getName()),
            isSilent,
            firstJoin,
                Formatter.serialize(formattedMessage),
                Formatter.sanitize(formattedMessage)
        );
        core.getDiscordWebhookIntegration().onNetworkJoin(networkJoinEvent);
        plugin.fireEvent(networkJoinEvent);
    }

    /**
     * Handles a player swapping between servers within the network
     * @param player Player that swapped
     * @param server Server the player swapped to
     * @param fromLimbo True if the player swapped from a LimboAPI server
     */
    private void handlePlayerSwap(@NotNull CorePlayer player, @NotNull CoreBackendServer server, boolean fromLimbo) {
        player.setLastKnownConnectedServer(server);

        if (storage.getShouldSuppressLimboSwap() && fromLimbo) {
            return;
        }

        String to = server.getName();
        String from = storage.getFrom(player);

        if (!storage.isSwapServerMessageEnabled()) {
            return;
        }

        if (storage.isBlacklisted(from, to)) {
            return;
        }

        String message = MessageHandler.getInstance()
            .parseSwitchMessage(player, from, to);

        // Silent
        boolean isSilent = isSilentEvent(player);

        if (isSilent) {
            MessageHandler.getInstance().broadcastSilentMessage(message, MessageType.SWAP, from, to, player);
        } else {
            MessageHandler.getInstance()
                .broadcastMessage(message, MessageType.SWAP, from, to, player);
        }

        Component formattedMessage = Formatter.deserialize(message);
        // Call the custom ServerSwapEvent
        SwapServerEvent swapServerEvent = new SwapServerEvent(
            player,
            storage.getServerDisplayName(from),
            storage.getServerDisplayName(to),
            isSilent,
                Formatter.serialize(formattedMessage),
                Formatter.sanitize(formattedMessage)
        );
        core.getDiscordWebhookIntegration().onSwapServer(swapServerEvent);
        plugin.fireEvent(swapServerEvent);
    }

    /**
     * Called before a full connection is made to a server
     * Used to determine the player's previous server should they be swapping
     * @param player Trigger player
     * @param previousServerName Previous server name
     */
    public void onPreConnect(@NotNull CorePlayer player, @Nullable String previousServerName) {
        if (previousServerName != null) {
            storage.setFrom(player, previousServerName);
        }
    }

    /**
     * Called somewhat after a player is fully connected to a server
     * 'somewhat' because on Velocity the Player#getCurrentServer method will sometimes still not contain a server
     * at this stage
     * @param player Trigger player
     * @param server Connected server
     * @param previousServer Previously connected server - only null on join or when swapping from a LimboAPI server
     */
    public void onServerConnected(@NotNull CorePlayer player, @NotNull CoreBackendServer server, @Nullable CoreBackendServer previousServer) {
        plugin.runTaskAsync(() -> {
            if (!storage.isConnected(player)) {
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

    /**
     * Called when a player disconnects from the network
     * @param player Trigger player
     */
    public void onDisconnect(@NotNull CorePlayer player) {

        if (!storage.isConnected(player)) {
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
            return;
        }

        storage.setConnected(player, false);

        if (!storage.isLeaveNetworkMessageEnabled()) {
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
            return;
        }

        if (storage.isBlacklisted(player)) {
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
            return;
        }

        if (storage.getShouldSuppressLimboLeave() && player.isInLimbo()) {
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
            return;
        }

        String message = MessageHandler.getInstance().formatLeaveMessage(player);

        // Silent
        boolean isSilent = isSilentEvent(player);

        if (isSilent) {
            core.SilentEvent("QUIT", player.getName());
            if (storage.getNotifyAdminsOnSilentMove()) {
                for (CorePlayer p : plugin.getAllPlayers()) {
                    if (p.hasPermission("networkjoinmessages.silent")) {
                        MessageHandler.getInstance().sendMessage(p, storage.getSilentPrefix() + message, player);
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
            storage.getServerDisplayName(player.getCurrentServer().getName()),
            isSilent,
                Formatter.serialize(formattedMessage),
                Formatter.sanitize(formattedMessage)
        );
        core.getDiscordWebhookIntegration().onNetworkQuit(networkQuitEvent);
        plugin.fireEvent(networkQuitEvent);

        plugin.getPlayerManager().removePlayer(player.getUniqueId());
    }
}
