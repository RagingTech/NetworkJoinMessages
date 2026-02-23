package xyz.earthcow.networkjoinmessages.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.broadcast.ReceiverResolver;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.player.PlayerStateStore;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;
import xyz.earthcow.networkjoinmessages.common.util.PlaceholderResolver;

import java.util.*;

/**
 * Dispatches formatted message strings to the correct recipients.
 * Has no knowledge of message template formatting or player-count computation —
 * those responsibilities live in {@link xyz.earthcow.networkjoinmessages.common.broadcast.MessageFormatter}.
 */
public final class MessageHandler {

    private final CorePlugin plugin;
    private final PluginConfig config;
    private final PlayerStateStore stateStore;
    private final PlaceholderResolver placeholderResolver;
    private final ReceiverResolver receiverResolver;

    public MessageHandler(
            CorePlugin plugin,
            PluginConfig config,
            PlayerStateStore stateStore,
            PlaceholderResolver placeholderResolver,
            ReceiverResolver receiverResolver
    ) {
        this.plugin = plugin;
        this.config = config;
        this.stateStore = stateStore;
        this.placeholderResolver = placeholderResolver;
        this.receiverResolver = receiverResolver;
    }

    // --- Single-recipient send ---

    /**
     * Sends a message to a command sender. If the sender is a player, they are also used as the
     * placeholder parse target. Designed for self-directed messages (e.g., command responses).
     */
    public void sendMessage(CoreCommandSender sender, String message) {
        CorePlayer parseTarget = sender instanceof CorePlayer p ? p : null;
        sendMessage(sender, message, parseTarget);
    }

    /**
     * Sends a message to a command sender, resolving placeholders against the given player.
     *
     * @param parseTarget the player to resolve placeholders against, or null to skip resolution
     */
    public void sendMessage(CoreCommandSender sender, String message, @Nullable CorePlayer parseTarget) {
        if (parseTarget != null) {
            placeholderResolver.resolve(message, parseTarget,
                formatted -> sender.sendMessage(Formatter.deserialize(formatted)));
        } else {
            sender.sendMessage(Formatter.deserialize(message));
        }
    }

    // --- Broadcast ---

    /** Broadcasts a non-silent message using the player's current server as context. */
    public void broadcastMessage(String text, MessageType type, CorePlayer parseTarget) {
        broadcastMessage(text, type, parseTarget, false);
    }

    /** Broadcasts a message using the player's current server as both from and to context. */
    public void broadcastMessage(String text, MessageType type, CorePlayer parseTarget, boolean silent) {
        broadcastMessage(text, type, parseTarget.getCurrentServer().getName(), "", parseTarget, silent);
    }

    /** Broadcasts a non-silent message with explicit server context. */
    public void broadcastMessage(String text, MessageType type, String from, String to, CorePlayer parseTarget) {
        broadcastMessage(text, type, from, to, parseTarget, false);
    }

    /**
     * Broadcasts a message to all appropriate recipients.
     *
     * <p>If {@code silent} is true, only the console and permission-holding admins receive it.
     * Otherwise, all players who pass receiver, blacklist, and suppression checks are notified.
     *
     * @param text        the formatted message template
     * @param type        the message type (determines receiver list)
     * @param from        the origin server name
     * @param to          the destination server name
     * @param parseTarget the player to resolve placeholders against (may be null for leave messages)
     * @param silent      whether this is a vanished/silent event
     */
    public void broadcastMessage(
            String text, MessageType type,
            String from, String to,
            @Nullable CorePlayer parseTarget,
            boolean silent
    ) {
        if (silent) {
            broadcastSilentMessage(text, type, from, to, parseTarget);
            return;
        }

        List<CorePlayer> receivers = switch (type) {
            case SWAP       -> receiverResolver.getSwapReceivers(to, from);
            case FIRST_JOIN -> receiverResolver.getFirstJoinReceivers(from);
            case JOIN       -> receiverResolver.getJoinReceivers(from);
            case LEAVE      -> receiverResolver.getLeaveReceivers(from);
        };

        sendMessage(plugin.getConsole(), text, parseTarget);

        // Merge per-player and per-server suppression sets into one for O(1) lookup
        Set<UUID> suppressed = new HashSet<>(stateStore.getSuppressedPlayers(type));
        suppressed.addAll(receiverResolver.getServerSuppressedPlayers(type));

        for (CorePlayer player : receivers) {
            if (!suppressed.contains(player.getUniqueId())) {
                sendMessage(player, text, parseTarget);
            }
        }
    }

    private void broadcastSilentMessage(
            @NotNull String text, @NotNull MessageType type,
            @NotNull String from, @NotNull String to,
            @Nullable CorePlayer parseTarget
    ) {
        sendSilentConsoleMessage(type, from, to, parseTarget);

        if (!config.isNotifyAdminsOnSilentMove()) return;

        for (CorePlayer player : plugin.getAllPlayers()) {
            if (player.hasPermission("networkjoinmessages.silent")) {
                sendMessage(player, config.getSilentPrefix() + text, parseTarget);
            }
        }
    }

    private void sendSilentConsoleMessage(
            MessageType type, String from, String to,
            @Nullable CorePlayer parseTarget
    ) {
        String message = switch (type) {
            case SWAP            -> config.getConsoleSilentSwap().replace("%to%", to).replace("%from%", from);
            case FIRST_JOIN, JOIN -> config.getConsoleSilentJoin();
            case LEAVE           -> config.getConsoleSilentLeave();
        };
        sendMessage(plugin.getConsole(), message, parseTarget);
    }
}
