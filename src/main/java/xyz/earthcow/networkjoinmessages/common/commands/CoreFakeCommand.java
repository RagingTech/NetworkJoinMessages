package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.Storage;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;

import java.util.List;

public class CoreFakeCommand implements Command {

    private final List<String> COMMAND_ARGS = ImmutableList.of(
        "fakejoin", "fakequit", "fakeswitch", "fj", "fq", "fs", "toggle"
    );

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (!(coreCommandSender instanceof CorePlayer)) {
            coreCommandSender.sendMessage("Only players can run this command!");
            return;
        }

        CorePlayer player = (CorePlayer) coreCommandSender;

        if (!player.hasPermission("networkjoinmessages.fakemessage")) {
            MessageHandler.getInstance().sendMessage(
                player,
                ConfigManager.getPluginConfig().getString("Messages.Commands.NoPermission")
            );
            return;
        }

        if (args.length < 1) {
            MessageHandler.getInstance().sendMessage(
                player,
                ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.NoArgument")
            );
            return;
        }

        String message;

        switch (args[0].toLowerCase()) {
            case "fakejoin":
            case "fj":
                message = MessageHandler.getInstance().formatJoinMessage(player);
                MessageHandler.getInstance().broadcastMessage(message, "join", player);
                return;
            case "fakequit":
            case "fq":
                message = MessageHandler.getInstance().formatLeaveMessage(player);
                MessageHandler.getInstance().broadcastMessage(message, "leave", player);
                return;
            case "fakeswitch":
            case "fs":
                if (args.length < 3) {
                    MessageHandler.getInstance().sendMessage(
                        player,
                        ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.FakeSwitchNoArgument")
                    );
                    return;
                }
                String fromName = args[1];
                String toName = args[2];

                message = MessageHandler.getInstance().parseSwitchMessage(player, fromName, toName);
                MessageHandler.getInstance().broadcastMessage(message, "switch", fromName, toName, player);
                return;
            case "toggle":
                if (!player.hasPermission("networkjoinmessages.silent")) {
                    MessageHandler.getInstance().sendMessage(
                        player,
                        ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.ToggleSilentNoPerm")
                    );
                    return;
                }
                boolean state = !Storage.getInstance().getSilentMessageState(player);
                Storage.getInstance().setSilentMessageState(player, state);
                MessageHandler.getInstance().sendMessage(
                    player,
                    ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.ToggleSilent")
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
                    return Storage.getInstance().getServerNames();
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