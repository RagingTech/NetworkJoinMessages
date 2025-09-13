package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.List;

public class CoreFakeCommand implements Command {

    private final List<String> COMMAND_ARGS = ImmutableList.of(
        "fakejoin", "fakequit", "fakeswitch", "fj", "fq", "fs", "toggle"
    );

    private final Storage storage;
    private final MessageHandler messageHandler;

    public CoreFakeCommand(Storage storage, MessageHandler messageHandler) {
        this.storage = storage;
        this.messageHandler = messageHandler;
    }

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (!(coreCommandSender instanceof CorePlayer player)) {
            coreCommandSender.sendMessage("Only players can run this command!");
            return;
        }

        if (!player.hasPermission("networkjoinmessages.fakemessage")) {
            messageHandler.sendMessage(
                player,
                ConfigManager.getPluginConfig().getString("Messages.Commands.NoPermission")
            );
            return;
        }

        if (args.length < 1) {
            messageHandler.sendMessage(
                player,
                ConfigManager.getPluginConfig().getString("Messages.Commands.FakeMessage.NoArgument")
            );
            return;
        }

        String message;

        switch (args[0].toLowerCase()) {
            case "fakejoin":
            case "fj":
                message = messageHandler.formatJoinMessage(player);
                messageHandler.broadcastMessage(message, MessageType.JOIN, player);
                return;
            case "fakequit":
            case "fq":
                message = messageHandler.formatLeaveMessage(player);
                messageHandler.broadcastMessage(message, MessageType.LEAVE, player);
                return;
            case "fakeswitch":
            case "fs":
                if (args.length < 3) {
                    messageHandler.sendMessage(
                        player,
                        ConfigManager.getPluginConfig().getString("Messages.Commands.FakeMessage.FakeSwitchNoArgument")
                    );
                    return;
                }
                String fromName = args[1];
                String toName = args[2];

                message = messageHandler.parseSwitchMessage(player, fromName, toName);
                messageHandler.broadcastMessage(message, MessageType.SWAP, fromName, toName, player);
                return;
            case "toggle":
                if (!player.hasPermission("networkjoinmessages.silent")) {
                    messageHandler.sendMessage(
                        player,
                        ConfigManager.getPluginConfig().getString("Messages.Commands.FakeMessage.ToggleSilentNoPerm")
                    );
                    return;
                }
                boolean state = !storage.getSilentMessageState(player);
                storage.setSilentMessageState(player, state);
                messageHandler.sendMessage(
                    player,
                    ConfigManager.getPluginConfig().getString("Messages.Commands.FakeMessage.ToggleSilent")
                        .replaceAll("<state>", String.valueOf(state))
                );
        }
    }

    @Override
    public String getRequiredPermission() {
        return "networkjoinmessages.fakemessage";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        switch (args.length) {
            case 0:
            case 1:
                return COMMAND_ARGS;
            case 2:
            case 3:
                if (args[0].equalsIgnoreCase("fs") || args[0].equalsIgnoreCase("fakeswitch")) {
                    return storage.getServerNames();
                } else {
                    return ImmutableList.of(
                        ConfigManager.getPluginConfig().getString("Messages.Commands.NoMoreArgumentsNeeded")
                    );
                }
            default:
                return ImmutableList.of(
                    ConfigManager.getPluginConfig().getString("Messages.Commands.NoMoreArgumentsNeeded")
                );
        }
    }
}