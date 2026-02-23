package xyz.earthcow.networkjoinmessages.common.util;

import net.kyori.adventure.text.Component;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.broadcast.MessageFormatter;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;

public class SpoofManager {

    private final CorePlugin plugin;
    private final PluginConfig config;
    private final MessageHandler messageHandler;
    private final MessageFormatter messageFormatter;

    public SpoofManager(CorePlugin plugin, PluginConfig config, MessageHandler messageHandler, MessageFormatter messageFormatter) {
        this.plugin = plugin;
        this.config = config;
        this.messageHandler = messageHandler;
        this.messageFormatter = messageFormatter;
    }

    public void spoofJoin(CorePlayer player) {
        plugin.getCoreLogger().debug("Spoofing join for " + player.getName());
        String message = messageFormatter.formatJoinMessage(player);
        Component component = Formatter.deserialize(message);
        messageHandler.broadcastMessage(message, MessageType.JOIN, player);
        String serverName = player.getCurrentServer().getName();
        plugin.fireEvent(new NetworkJoinEvent(
            player, serverName, config.getServerDisplayName(serverName),
            false, false,
            Formatter.serialize(component), Formatter.sanitize(component)
        ));
    }

    public void spoofLeave(CorePlayer player) {
        plugin.getCoreLogger().debug("Spoofing leave for " + player.getName());
        String message = messageFormatter.formatLeaveMessage(player);
        Component component = Formatter.deserialize(message);
        messageHandler.broadcastMessage(message, MessageType.LEAVE, player);
        String serverName = player.getCurrentServer().getName();
        plugin.fireEvent(new NetworkLeaveEvent(
            player, serverName, config.getServerDisplayName(serverName),
            false,
            Formatter.serialize(component), Formatter.sanitize(component)
        ));
    }

    public void spoofSwap(CorePlayer player, String from, String to) {
        plugin.getCoreLogger().debug("Spoofing swap for " + player.getName());
        String message = messageFormatter.formatSwapMessage(player, from, to);
        Component component = Formatter.deserialize(message);
        messageHandler.broadcastMessage(message, MessageType.SWAP, from, to, player);
        plugin.fireEvent(new SwapServerEvent(
            player, from, to,
            config.getServerDisplayName(from), config.getServerDisplayName(to),
            false,
            Formatter.serialize(component), Formatter.sanitize(component)
        ));
    }
}
