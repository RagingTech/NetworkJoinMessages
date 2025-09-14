package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

import java.util.List;

public class CoreToggleJoinCommand implements Command {

    private final List<String> COMMAND_ARGS = ImmutableList.of(
        "join", "leave", "quit", "switch", "all"
    );
    
    private final Storage storage;
    private final MessageHandler messageHandler;

    public CoreToggleJoinCommand(Storage storage, MessageHandler messageHandler) {
        this.storage = storage;
        this.messageHandler = messageHandler;
    }
    
    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (!(coreCommandSender instanceof CorePlayer player)) {
            coreCommandSender.sendMessage("Only players can run this command!");
            return;
        }

        if (!player.hasPermission("networkjoinmessages.togglemessage")) {
            messageHandler.sendMessage(
                player,
                ConfigManager.getPluginConfig().getString("Messages.Commands.NoPermission")
            );
            return;
        }

        if (args.length < 1) {
            messageHandler.sendMessage(
                player,
                ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.MissingFirstArgument")
            );
            return;
        }

        if (args.length < 2) {
            messageHandler.sendMessage(
                player,
                ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.MissingState")
            );
            return;
        }

        String mode = args[0].toLowerCase();
        boolean state = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");

        if (!COMMAND_ARGS.contains(mode)) {
            messageHandler.sendMessage(
                player,
                ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.MissingFirstArgument")
            );
            return;
        }

        storage.setSendMessageState(mode, player.getUniqueId(), state);

        messageHandler.sendMessage(
            player,
            ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.Confirmation")
                .replaceAll("<mode>", mode)
                .replaceAll("<state>", String.valueOf(state))
        );

    }

    @Override
    public String getRequiredPermission() {
        return "networkjoinmessages.togglemessage";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        switch (args.length) {
            case 0:
            case 1:
                return COMMAND_ARGS;
            case 2:
                return ImmutableList.of("on", "off");
            default:
                return ImmutableList.of("No more arguments needed.");
        }
    }
}