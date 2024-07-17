package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.Storage;
import xyz.earthcow.networkjoinmessages.common.util.HexChat;

import java.util.List;

public class ToggleJoinCommand implements Command {

    List<String> commandArguments = ImmutableList.of(
        "join",
        "leave",
        "quit",
        "switch",
        "all"
    );

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (!(coreCommandSender instanceof CorePlayer)) {
            return;
        }
        CorePlayer player = (CorePlayer) coreCommandSender;
        if (!player.hasPermission("networkjoinmessages.togglemessage")) {
            String msg = ConfigManager.getPluginConfig().getString("Messages.Commands.NoPermission");
            player.sendMessage(HexChat.translateHexCodes(msg));
            return;
        }

        if (args.length < 1) {
            String msg = ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.MissingFirstArgument");
            player.sendMessage(HexChat.translateHexCodes(msg));
            return;
        }

        if (args.length < 2) {
            String msg = ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.MissingState");
            player.sendMessage(HexChat.translateHexCodes(msg));
            return;
        }

        boolean state = false;
        if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true")) {
            state = true;
        }

        if (!commandArguments.contains(args[0].toLowerCase())) {
            //Triggered if commandArguments does not contain the argument used.
            String msg = ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.MissingFirstArgument");
            player.sendMessage(HexChat.translateHexCodes(msg));
            return;
        }

        Storage.getInstance().setSendMessageState(args[0], player.getUniqueId(), state);
        String msg = ConfigManager.getPluginConfig().getString("Messages.Commands.ToggleJoin.Confirmation");
        msg = msg.replace("<mode>", args[0]);
        msg = msg.replace("<state>", state + "");
        player.sendMessage(HexChat.translateHexCodes(msg));
    }

    @Override
    public String getRequiredPermssion() {
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
