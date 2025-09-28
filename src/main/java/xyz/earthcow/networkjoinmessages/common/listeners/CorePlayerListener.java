package xyz.earthcow.networkjoinmessages.common.listeners;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.modules.SayanVanishHook;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.H2PlayerJoinTracker;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

public class CorePlayerListener {

    private final CorePlugin plugin;
    private final Storage storage;
    private final MessageHandler messageHandler;

    private H2PlayerJoinTracker firstJoinTracker;

    @Nullable
    private final SayanVanishHook sayanVanishHook;

    @Nullable
    private final PremiumVanish premiumVanish;
    
    public CorePlayerListener(CorePlugin plugin, Storage storage, MessageHandler messageHandler, @Nullable SayanVanishHook sayanVanishHook) {
        this.plugin = plugin;
        this.storage = storage;
        this.messageHandler = messageHandler;
        this.sayanVanishHook = sayanVanishHook;
        this.premiumVanish = plugin.getVanishAPI();

        try {
            this.firstJoinTracker = new H2PlayerJoinTracker(plugin.getCoreLogger(), "./" + plugin.getDataFolder().getPath() + "/joined");
        } catch (Exception ex) {
            plugin.getCoreLogger().severe("Failed to load H2 first join tracker!");
            plugin.getCoreLogger().debug("Exception: " + ex);
        }

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
                (sayanVanishHook != null && storage.isSVTreatVanishedPlayersAsSilent() && sayanVanishHook.isVanished(player))
                ||
                (premiumVanish != null && storage.isPVTreatVanishedPlayersAsSilent() && premiumVanish.isVanished(player.getUniqueId()));
    }

    private boolean shouldNotBroadcast(@NotNull CorePlayer player, @NotNull MessageType type) {
        return shouldNotBroadcast(player, type, "", "", false);
    }

    private boolean shouldNotBroadcast(@NotNull CorePlayer player, @NotNull MessageType type, @NotNull String from, @NotNull String to, boolean fromLimbo) {
        if (player.getCurrentServer() == null) {
            plugin.getCoreLogger().debug("Player, " + player.getName() + ", has no current server. No message will be" +
                " sent for them. This typically indicates the server they attempted to join was unavailable. If this" +
                " is a mistake, please report it to the developer by creating a new issue" +
                " https://github.com/RagingTech/NetworkJoinMessages/issues/new");
            return true;
        }

        switch (type) {
            case SWAP -> {
                if (storage.isShouldSuppressLimboSwap() && fromLimbo) {
                    plugin.getCoreLogger().debug("Skipping " + player.getName() +
                        " - suppress limbo swap");
                    return true;
                }

                if (!storage.isSwapServerMessageEnabled()) {
                    plugin.getCoreLogger().debug("Skipping " + player.getName() +
                        " - swap message is disabled");
                    return true;
                }

                if (storage.isBlacklisted(from, to)) {
                    plugin.getCoreLogger().debug("Skipping " + player.getName() +
                        " - blacklisted from " + from + " to " + to);
                    return true;
                }
            }
            case FIRST_JOIN, JOIN -> {
                boolean firstJoin = type.equals(MessageType.FIRST_JOIN);

                if (firstJoin) {
                    firstJoinTracker.markAsJoined(player.getUniqueId(), player.getName());
                    if (!storage.isFirstJoinNetworkMessageEnabled()) {
                        plugin.getCoreLogger().debug("Skipping " + player.getName() +
                            " - first join message is disabled");
                        return true;
                    }
                } else if (!storage.isJoinNetworkMessageEnabled()) {
                    plugin.getCoreLogger().debug("Skipping " + player.getName() +
                        " - join message is disabled");
                    return true;
                }

                // Blacklist Check
                if (storage.isBlacklisted(player)) {
                    plugin.getCoreLogger().debug("Skipping " + player.getName() +
                        " - blacklist check failed; server: " + player.getCurrentServer().getName());
                    return true;
                }

                if (storage.isShouldSuppressLimboJoin() && player.isInLimbo()) {
                    plugin.getCoreLogger().debug("Skipping " + player.getName() +
                        " - suppress limbo join");
                    return true;
                }
            }
            case LEAVE -> {
                if (!storage.isConnected(player) || !storage.isLeaveNetworkMessageEnabled() || storage.isBlacklisted(player)) {
                    plugin.getCoreLogger().debug("Skipping " + player.getName() +
                        " - already disconnected or leave message is disabled or blacklisted; server: " +
                        player.getCurrentServer().getName());
                    return true;
                }

                if (storage.isShouldSuppressLimboLeave() && player.isInLimbo()) {
                    plugin.getCoreLogger().debug("Skipping " + player.getName() +
                        " - suppress limbo leave");
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

        boolean firstJoin = !firstJoinTracker.hasJoined(player.getUniqueId());
        MessageType msgType = firstJoin ? MessageType.FIRST_JOIN : MessageType.JOIN;

        if (shouldNotBroadcast(player, msgType)) {
            return;
        }

        String message = firstJoin ? messageHandler.formatFirstJoinMessage(player) : messageHandler.formatJoinMessage(player);

        boolean isSilent = isSilentEvent(player);

        if (isSilent) {
            if (player.hasPermission("networkjoinmessages.spoof")) {
                messageHandler.sendMessage(player, storage.getSpoofJoinNotification());
            }
        }

        messageHandler.broadcastMessage(message, msgType, player, isSilent);

        Component formattedMessage = Formatter.deserialize(message);
        // All checks have passed to reach this point
        // Call the custom NetworkJoinEvent
        NetworkJoinEvent networkJoinEvent = new NetworkJoinEvent(
            player,
            server.getName(),
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
            .parseSwapMessage(player, from, to);

        // Silent
        boolean isSilent = isSilentEvent(player);

        // Broadcast message
        messageHandler.broadcastMessage(message, MessageType.SWAP, from, to, player, isSilent);

        Component formattedMessage = Formatter.deserialize(message);
        // Call the custom SwapServerEvent
        SwapServerEvent swapServerEvent = new SwapServerEvent(
            player,
            from,
            to,
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
        // Call the custom NetworkLeaveEvent
        NetworkLeaveEvent networkLeaveEvent = new NetworkLeaveEvent(
            player,
            player.getCurrentServer().getName(),
            storage.getServerDisplayName(player.getCurrentServer().getName()),
            isSilent,
            Formatter.serialize(formattedMessage),
            Formatter.sanitize(formattedMessage)
        );
        plugin.fireEvent(networkLeaveEvent);

        plugin.getPlayerManager().removePlayer(player.getUniqueId());
        storage.setConnected(player, false);
    }

    public H2PlayerJoinTracker getPlayerJoinTracker() {
        return firstJoinTracker;
    }
}
