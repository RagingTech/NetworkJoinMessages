package xyz.earthcow.networkjoinmessages.common.util;

import net.kyori.adventure.text.Component;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;

public class SpoofManager {
    private final CorePlugin plugin;
    private final CoreLogger logger;
    private final Storage storage;
    private final MessageHandler messageHandler;

    public SpoofManager(CorePlugin plugin, Storage storage, MessageHandler messageHandler) {
        this.plugin = plugin;
        this.logger = plugin.getCoreLogger();
        this.storage = storage;
        this.messageHandler = messageHandler;
    }

    public void spoofJoin(CorePlayer player) {
        logger.debug("Spoofing join message for player: " + player.getName());
        String message = messageHandler.formatJoinMessage(player);
        Component formattedMessage = Formatter.deserialize(message);

        messageHandler.broadcastMessage(message, MessageType.JOIN, player);

        String currentServerName = player.getCurrentServer().getName();
        String currentServerDisplayName = storage.getServerDisplayName(currentServerName);

        plugin.fireEvent(new NetworkJoinEvent(
                player, currentServerName, currentServerDisplayName, false, false,
                Formatter.serialize(formattedMessage),
                Formatter.sanitize(formattedMessage)
        ));
    }

    public void spoofLeave(CorePlayer player) {
        logger.debug("Spoofing leave message for player: " + player.getName());
        String message = messageHandler.formatLeaveMessage(player);
        Component formattedMessage = Formatter.deserialize(message);

        messageHandler.broadcastMessage(message, MessageType.LEAVE, player);

        String currentServerName = player.getCurrentServer().getName();
        String currentServerDisplayName = storage.getServerDisplayName(currentServerName);

        plugin.fireEvent(new NetworkLeaveEvent(
                player, currentServerName, currentServerDisplayName, false,
                Formatter.serialize(formattedMessage),
                Formatter.sanitize(formattedMessage)
        ));
    }

    public void spoofSwap(CorePlayer player, String from, String to) {
        logger.debug("Spoofing swap message for player: " + player.getName());
        String fromDisplayName = storage.getServerDisplayName(from);
        String toDisplayName = storage.getServerDisplayName(to);

        String message = messageHandler.parseSwapMessage(player, from, to);

        Component formattedMessage = Formatter.deserialize(message);

        messageHandler.broadcastMessage(message, MessageType.SWAP, from, to, player);

        plugin.fireEvent(new SwapServerEvent(
                player, from, to, fromDisplayName, toDisplayName, false,
                Formatter.serialize(formattedMessage),
                Formatter.sanitize(formattedMessage)
        ));
    }

}
