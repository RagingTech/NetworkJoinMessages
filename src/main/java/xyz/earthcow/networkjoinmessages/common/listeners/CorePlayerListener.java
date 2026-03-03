package xyz.earthcow.networkjoinmessages.common.listeners;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.broadcast.MessageFormatter;
import xyz.earthcow.networkjoinmessages.common.broadcast.ReceiverResolver;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.player.*;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.PlayerJoinTracker;
import xyz.earthcow.networkjoinmessages.common.MessageType;
import xyz.earthcow.networkjoinmessages.common.util.PlaceholderResolver;

/**
 * Routes platform-level player events (join, swap, disconnect) to the appropriate handlers.
 * Contains no business logic — delegates to focused collaborators for silence checking,
 * message formatting, buffer management, and dispatching.
 */
public class CorePlayerListener {

    private final CorePlugin plugin;
    private final PluginConfig config;
    private final PlayerStateStore stateStore;
    private final MessageHandler messageHandler;
    private final MessageFormatter messageFormatter;
    private final ReceiverResolver receiverResolver;
    private final SilenceChecker silenceChecker;
    private final LeaveMessageCache leaveMessageCache;
    private final LeaveJoinBufferManager leaveJoinBuffer;
    private final PlaceholderResolver placeholderResolver;
    private final PlayerJoinTracker firstJoinTracker;

    public CorePlayerListener(
            CorePlugin plugin,
            PluginConfig config,
            PlayerStateStore stateStore,
            MessageHandler messageHandler,
            MessageFormatter messageFormatter,
            ReceiverResolver receiverResolver,
            SilenceChecker silenceChecker,
            LeaveMessageCache leaveMessageCache,
            LeaveJoinBufferManager leaveJoinBuffer,
            PlaceholderResolver placeholderResolver,
            PlayerJoinTracker firstJoinTracker
    ) {
        this.plugin = plugin;
        this.config = config;
        this.stateStore = stateStore;
        this.messageHandler = messageHandler;
        this.messageFormatter = messageFormatter;
        this.receiverResolver = receiverResolver;
        this.silenceChecker = silenceChecker;
        this.leaveMessageCache = leaveMessageCache;
        this.leaveJoinBuffer = leaveJoinBuffer;
        this.placeholderResolver = placeholderResolver;
        this.firstJoinTracker = firstJoinTracker;
    }

    // --- Public event entry points ---

    /**
     * Called before the player fully connects, recording the previous server for swap detection.
     */
    public void onPreConnect(@NotNull CorePlayer player, @Nullable String previousServerName) {
        if (previousServerName != null) {
            stateStore.setFrom(player, previousServerName);
        }
    }

    /**
     * Called when a player has fully connected to a backend server.
     *
     * @param player         the connected player
     * @param server         the server they connected to
     * @param previousServer null on first join or when coming from a LimboAPI server
     */
    public void onServerConnected(@NotNull CorePlayer player, @NotNull CoreBackendServer server,
                                   @Nullable CoreBackendServer previousServer) {
        plugin.runTaskAsync(() -> {
            if (!stateStore.isConnected(player)) {
                handleJoin(player, server);
            } else {
                boolean fromLimbo = plugin.getServerType() == ServerType.VELOCITY && previousServer == null;
                handleSwap(player, server, fromLimbo);
            }
        });
    }

    /**
     * Called when a player disconnects from the network.
     */
    public void onDisconnect(@NotNull CorePlayer player) {
        if (player.isDisconnecting()) {
            plugin.getCoreLogger().debug("Duplicate disconnect ignored for " + player.getName());
            return;
        }
        player.setDisconnecting();

        if (shouldSkipLeave(player)) {
            cleanup(player);
            return;
        }

        broadcastLeave(player);
        cleanup(player);
    }

    // --- Private handlers ---

    private void handleJoin(@NotNull CorePlayer player, @NotNull CoreBackendServer server) {
        stateStore.setConnected(player, true);
        player.setLastKnownConnectedServer(server);

        PremiumVanish pv = plugin.getVanishAPI();
        if (pv != null && pv.isVanished(player.getUniqueId())) {
            player.setPremiumVanishHidden(true);
        }

        leaveMessageCache.refresh(player);
        leaveMessageCache.startFor(player);

        boolean firstJoin = firstJoinTracker != null && !firstJoinTracker.hasJoined(player.getUniqueId());
        MessageType msgType = firstJoin ? MessageType.FIRST_JOIN : MessageType.JOIN;

        if (shouldSkipJoin(player, msgType, firstJoin)) return;

        String message = firstJoin
            ? messageFormatter.formatFirstJoinMessage(player)
            : messageFormatter.formatJoinMessage(player);

        boolean silent = silenceChecker.isSilent(player);

        if (silent && player.hasPermission("networkjoinmessages.spoof")) {
            messageHandler.sendMessage(player, config.getSpoofJoinNotification());
        }

        messageHandler.broadcastMessage(message, msgType, player, silent);
        fireJoinEvent(player, server, message, silent, firstJoin);
    }

    private void handleSwap(@NotNull CorePlayer player, @NotNull CoreBackendServer server, boolean fromLimbo) {
        player.setLastKnownConnectedServer(server);
        leaveMessageCache.refresh(player);

        String to   = server.getName();
        String from = stateStore.getFrom(player);

        if (shouldSkipSwap(player, from, to, fromLimbo)) return;

        String message = messageFormatter.formatSwapMessage(player, from, to);
        boolean silent = silenceChecker.isSilent(player);

        messageHandler.broadcastMessage(message, MessageType.SWAP, from, to, player, silent);
        fireSwapEvent(player, from, to, message, silent);
    }

    private void broadcastLeave(@NotNull CorePlayer player) {
        String message = player.getCachedLeaveMessage();
        boolean silent = silenceChecker.isSilent(player);
        String serverName = player.getCurrentServer().getName();

        // Pass null as parseTarget — player is gone, placeholders already resolved in cache
        messageHandler.broadcastMessage(message, MessageType.LEAVE, serverName, "", null, silent);
        fireLeaveEvent(player, serverName, message, silent);
    }

    // --- Guard checks ---

    private boolean shouldSkipJoin(CorePlayer player, MessageType type, boolean firstJoin) {
        if (player.getCurrentServer() == null) {
            plugin.getCoreLogger().debug("Skipping " + player.getName() + " — no current server (server likely unavailable)");
            return true;
        }
        if (firstJoin) {
            if (firstJoinTracker != null) firstJoinTracker.markAsJoined(player.getUniqueId(), player.getName());
            if (!config.isFirstJoinNetworkMessageEnabled()) {
                plugin.getCoreLogger().debug("Skipping " + player.getName() + " — first-join message disabled");
                return true;
            }
        } else if (!config.isJoinNetworkMessageEnabled()) {
            plugin.getCoreLogger().debug("Skipping " + player.getName() + " — join message disabled");
            return true;
        }
        if (receiverResolver.isBlacklisted(player)) {
            plugin.getCoreLogger().debug("Skipping " + player.getName() + " — blacklisted on " + player.getCurrentServer().getName());
            return true;
        }
        if (config.isShouldSuppressLimboJoin() && player.isInLimbo()) {
            plugin.getCoreLogger().debug("Skipping " + player.getName() + " — suppress limbo join");
            return true;
        }
        if (leaveJoinBuffer.cancelIfPending(player)) {
            plugin.getCoreLogger().debug("Skipping " + player.getName() + " — rejoined within buffer window");
            return true;
        }
        return false;
    }

    private boolean shouldSkipSwap(CorePlayer player, String from, String to, boolean fromLimbo) {
        if (player.getCurrentServer() == null) return true;
        if (config.isShouldSuppressLimboSwap() && fromLimbo) {
            plugin.getCoreLogger().debug("Skipping " + player.getName() + " — suppress limbo swap");
            return true;
        }
        if (!config.isSwapServerMessageEnabled()) {
            plugin.getCoreLogger().debug("Skipping " + player.getName() + " — swap message disabled");
            return true;
        }
        if (receiverResolver.isBlacklisted(from, to)) {
            plugin.getCoreLogger().debug("Skipping " + player.getName() + " — blacklisted swap " + from + " -> " + to);
            return true;
        }
        return false;
    }

    private boolean shouldSkipLeave(CorePlayer player) {
        if (player.getCurrentServer() == null) return true;
        if (!stateStore.isConnected(player)) {
            plugin.getCoreLogger().debug("Skipping leave for " + player.getName() + " — not marked as connected");
            return true;
        }
        if (!config.isLeaveNetworkMessageEnabled()) {
            plugin.getCoreLogger().debug("Skipping leave for " + player.getName() + " — leave message disabled");
            return true;
        }
        if (receiverResolver.isBlacklisted(player)) {
            plugin.getCoreLogger().debug("Skipping leave for " + player.getName() + " — blacklisted");
            return true;
        }
        if (config.isShouldSuppressLimboLeave() && player.isInLimbo()) {
            plugin.getCoreLogger().debug("Skipping leave for " + player.getName() + " — suppress limbo leave");
            return true;
        }
        if (!leaveJoinBuffer.isDisabled() && !leaveJoinBuffer.isPending(player)) {
            leaveJoinBuffer.scheduleLeave(player, () -> broadcastLeave(player));
            plugin.getCoreLogger().debug("Buffering leave for " + player.getName());
            return true;
        }
        return false;
    }

    // --- Event firing ---

    private void fireJoinEvent(CorePlayer player, CoreBackendServer server, String message, boolean silent, boolean firstJoin) {
        Component component = Formatter.deserialize(message, player, placeholderResolver.getMiniPlaceholders());
        plugin.fireEvent(new NetworkJoinEvent(
            player,
            server.getName(),
            config.getServerDisplayName(server.getName()),
            silent, firstJoin,
            Formatter.serialize(component),
            Formatter.sanitize(component)
        ));
    }

    private void fireSwapEvent(CorePlayer player, String from, String to, String message, boolean silent) {
        Component component = Formatter.deserialize(message, player, placeholderResolver.getMiniPlaceholders());
        plugin.fireEvent(new SwapServerEvent(
            player, from, to,
            config.getServerDisplayName(from),
            config.getServerDisplayName(to),
            silent,
            Formatter.serialize(component),
            Formatter.sanitize(component)
        ));
    }

    private void fireLeaveEvent(CorePlayer player, String serverName, String message, boolean silent) {
        Component component = Formatter.deserialize(message, null, placeholderResolver.getMiniPlaceholders());
        plugin.fireEvent(new NetworkLeaveEvent(
            player, serverName,
            config.getServerDisplayName(serverName),
            silent,
            Formatter.serialize(component),
            Formatter.sanitize(component)
        ));
    }

    private void cleanup(CorePlayer player) {
        plugin.getPlayerManager().removePlayer(player.getUniqueId());
        stateStore.setConnected(player, false);
        leaveMessageCache.stopFor(player);
    }
}
