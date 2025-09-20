package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.List;

public class CoreSpoofCommand implements Command {

    private final List<String> COMMAND_ARGS = ImmutableList.of(
        "join", "leave", "swap", "toggle"
    );

    private final CorePlugin plugin;
    private final Storage storage;
    private final MessageHandler messageHandler;

    public CoreSpoofCommand(CorePlugin plugin, Storage storage, MessageHandler messageHandler) {
        this.plugin = plugin;
        this.storage = storage;
        this.messageHandler = messageHandler;
    }

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (!(coreCommandSender instanceof CorePlayer player)) {
            coreCommandSender.sendMessage("Only players can run this command!");
            return;
        }

        if (!player.hasPermission("networkjoinmessages.spoof")) {
            messageHandler.sendMessage(
                player,
                storage.getNoPermission()
            );
            return;
        }

        if (args.length < 1) {
            messageHandler.sendMessage(
                player,
                storage.getSpoofNoArgument()
            );
            return;
        }

        String currentServerName = player.getCurrentServer().getName();
        String currentServerDisplayName = storage.getServerDisplayName(currentServerName);

        String message;
        Component formattedMessage;

        switch (args[0].toLowerCase()) {
            case "join":
                message = messageHandler.formatJoinMessage(player);
                formattedMessage = Formatter.deserialize(message);

                messageHandler.broadcastMessage(message, MessageType.JOIN, player);

                plugin.fireEvent(new NetworkJoinEvent(
                    player, currentServerName, currentServerDisplayName, false, false,
                    Formatter.serialize(formattedMessage),
                    Formatter.sanitize(formattedMessage)
                ));
                return;
            case "leave":
                message = messageHandler.formatLeaveMessage(player);
                formattedMessage = Formatter.deserialize(message);

                messageHandler.broadcastMessage(message, MessageType.LEAVE, player);

                plugin.fireEvent(new NetworkLeaveEvent(
                    player, currentServerName, currentServerDisplayName, false,
                    Formatter.serialize(formattedMessage),
                    Formatter.sanitize(formattedMessage)
                ));
                return;
            case "swap":
                if (args.length < 3) {
                    messageHandler.sendMessage(
                        player,
                        storage.getSpoofSwapNoArgument()
                    );
                    return;
                }
                String fromName = args[1];
                String toName = args[2];

                String fromDisplayName = storage.getServerDisplayName(fromName);
                String toDisplayName = storage.getServerDisplayName(toName);

                try {
                    message = messageHandler.parseSwitchMessage(player, fromName, toName);
                } catch (NullPointerException e) {
                    messageHandler.sendMessage(
                        player,
                        "<red>Spoof requires valid server names if they contain player count placeholders."
                    );
                    return;
                }

                formattedMessage = Formatter.deserialize(message);

                messageHandler.broadcastMessage(message, MessageType.SWAP, fromName, toName, player);

                plugin.fireEvent(new SwapServerEvent(
                    player, fromName, toName, fromDisplayName, toDisplayName, false,
                    Formatter.serialize(formattedMessage),
                    Formatter.sanitize(formattedMessage)
                ));
                return;
            case "toggle":
                if (!player.hasPermission("networkjoinmessages.silent")) {
                    messageHandler.sendMessage(
                        player,
                        storage.getSpoofToggleSilentNoPerm()
                    );
                    return;
                }
                boolean state = !storage.getSilentMessageState(player);
                storage.setSilentMessageState(player, state);
                messageHandler.sendMessage(
                    player,
                    storage.getSpoofToggleSilent()
                        .replaceAll("%state%", String.valueOf(state))
                        .replaceAll("<state>", String.valueOf(state))
                );
        }
    }

    @Override
    public String getRequiredPermission() {
        return "networkjoinmessages.spoof";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        switch (args.length) {
            case 0:
            case 1:
                return COMMAND_ARGS;
            case 2:
            case 3:
                if (args[0].equalsIgnoreCase("swap")) {
                    return storage.getServerNames();
                } else {
                    return ImmutableList.of(
                        storage.getNoMoreArgumentsNeeded()
                    );
                }
            default:
                return ImmutableList.of(
                    storage.getNoMoreArgumentsNeeded()
                );
        }
    }
}