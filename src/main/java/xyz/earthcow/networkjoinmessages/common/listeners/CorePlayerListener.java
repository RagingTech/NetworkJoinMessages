package xyz.earthcow.networkjoinmessages.common.listeners;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.Core;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

public class CorePlayerListener {

    private final Core core;
    private final CorePlugin plugin;
    private final Storage storage;
    private final MessageHandler messageHandler;

    @Nullable
    private final PremiumVanish premiumVanish;
    
    public CorePlayerListener(Core core) {
        this.core = core;
        this.plugin = core.getPlugin();
        this.storage = core.getStorage();
        this.messageHandler = core.getMessageHandler();
        this.premiumVanish = plugin.getVanishAPI();
    }

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

    private boolean shouldNotBroadcast(@NotNull CorePlayer player, @NotNull MessageType type) {
        return shouldNotBroadcast(player, type, "", "", false);
    }

    private boolean shouldNotBroadcast(@NotNull CorePlayer player, @NotNull MessageType type, @NotNull String from, @NotNull String to, boolean fromLimbo) {
        switch (type) {
            case SWAP -> {
                if (storage.getShouldSuppressLimboSwap() && fromLimbo) {
                    return true;
                }

                if (!storage.isSwapServerMessageEnabled()) {
                    return true;
                }

                if (storage.isBlacklisted(from, to)) {
                    return true;
                }
            }
            case FIRST_JOIN, JOIN -> {
                boolean firstJoin = type.equals(MessageType.FIRST_JOIN);

                if (firstJoin) {
                    core.getFirstJoinTracker().markAsJoined(player.getUniqueId(), player.getName());
                    if (!storage.isFirstJoinNetworkMessageEnabled()) {
                        return true;
                    }
                } else if (!storage.isJoinNetworkMessageEnabled()) {
                    return true;
                }

                // Blacklist Check
                if (storage.isBlacklisted(player)) {
                    return true;
                }

                if (storage.getShouldSuppressLimboJoin() && player.isInLimbo()) {
                    return true;
                }
            }
            case LEAVE -> {
                if (!storage.isConnected(player) || !storage.isLeaveNetworkMessageEnabled() || storage.isBlacklisted(player)) {
                    return true;
                }

                if (storage.getShouldSuppressLimboLeave() && player.isInLimbo()) {
                    return true;
                }
            }
        }
        return false;
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
        MessageType msgType = firstJoin ? MessageType.FIRST_JOIN : MessageType.JOIN;

        if (shouldNotBroadcast(player, msgType)) {
            return;
        }

        String message = firstJoin ? core.getMessageHandler().formatFirstJoinMessage(player) : core.getMessageHandler().formatJoinMessage(player);

        boolean isSilent = isSilentEvent(player);

        if (isSilent) {
            if (player.hasPermission("networkjoinmessages.spoof")) {
                core.getFormatter().parsePlaceholdersAndThen(
                        ConfigManager.getPluginConfig().getString("Messages.Commands.Spoof.JoinNotification"),
                        player,
                        formattedMsg -> {
                            messageHandler.sendMessage(player, formattedMsg);
                        }
                );
            }
        }

        core.getMessageHandler().broadcastMessage(message, msgType, player, isSilent);

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

        String to = server.getName();
        String from = storage.getFrom(player);

        if (shouldNotBroadcast(player, MessageType.SWAP, from, to, fromLimbo)) {
            return;
        }

        String message = messageHandler
            .parseSwitchMessage(player, from, to);

        // Silent
        boolean isSilent = isSilentEvent(player);

        // Broadcast message
        messageHandler.broadcastMessage(message, MessageType.SWAP, from, to, player, isSilent);

        Component formattedMessage = Formatter.deserialize(message);
        // Call the custom SwapServerEvent
        SwapServerEvent swapServerEvent = new SwapServerEvent(
            player,
            storage.getServerDisplayName(from),
            storage.getServerDisplayName(to),
            isSilent,
            Formatter.serialize(formattedMessage),
            Formatter.sanitize(formattedMessage)
        );
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
            // If the player IS already connected, then they have just swapped servers

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

        if (shouldNotBroadcast(player, MessageType.LEAVE)) {
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
            storage.setConnected(player, false);
            return;
        }

        String message = messageHandler.formatLeaveMessage(player);

        // Silent
        boolean isSilent = isSilentEvent(player);

        // Broadcast message
        messageHandler.broadcastMessage(message, MessageType.LEAVE, player, isSilent);

        Component formattedMessage = Formatter.deserialize(message);
        // Call the custom NetworkQuitEvent
        NetworkLeaveEvent networkLeaveEvent = new NetworkLeaveEvent(
            player,
            storage.getServerDisplayName(player.getCurrentServer().getName()),
            isSilent,
            Formatter.serialize(formattedMessage),
            Formatter.sanitize(formattedMessage)
        );
        plugin.fireEvent(networkLeaveEvent);

        plugin.getPlayerManager().removePlayer(player.getUniqueId());
    }
}
