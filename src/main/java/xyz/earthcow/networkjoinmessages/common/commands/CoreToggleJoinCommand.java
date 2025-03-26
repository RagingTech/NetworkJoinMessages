package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.Storage;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;

import java.util.List;

public class CoreToggleJoinCommand implements Command {

    List<String> commandArguments = ImmutableList.of(
        "join", "leave", "quit", "switch", "all"
    );

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (!(coreCommandSender instanceof CorePlayer)) {
            return;
        }
        CorePlayer player = (CorePlayer) coreCommandSender;
        if (!player.hasPermission("networkjoinmessages.togglemessage")) {
            player.sendMessage(MessageHandler.getInstance().formatMessage(
                ConfigManager.getPluginConfig().getString("Messages.Commands.NoPermission"),
                player
            ));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(MessageHandler.getInstance().formatMessage(
                ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.MissingFirstArgument"),
                player
            ));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MessageHandler.getInstance().formatMessage(
                ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.MissingState"),
                player
            ));
            return;
        }

        boolean state = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");

        if (!commandArguments.contains(args[0].toLowerCase())) {
            player.sendMessage(MessageHandler.getInstance().formatMessage(
                ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.MissingFirstArgument"),
                player
            ));
            return;
        }

        Storage.getInstance().setSendMessageState(args[0], player.getUniqueId(), state);
        Component confirmationMessage = MessageHandler.getInstance().formatMessage(
            ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.Confirmation"),
            player
        ).replaceText(builder -> builder
            .matchLiteral("<mode>")
            .replacement(args[0])
            .matchLiteral("<state>")
            .replacement(String.valueOf(state))
        );
        player.sendMessage(confirmationMessage);
    }

    @Override
    public String getRequiredPermission() {
        return "networkjoinmessages.togglemessage";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        switch (args.length) {
            case 1:
                return commandArguments;
            case 2:
                return ImmutableList.of("on", "off");
            default:
                return ImmutableList.of("No more arguments needed.");
        }
    }
}