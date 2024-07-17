package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.Storage;
import xyz.earthcow.networkjoinmessages.common.util.HexChat;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;

import java.util.List;

public class CoreFakeCommand implements Command {

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (!(coreCommandSender instanceof CorePlayer)) {
            coreCommandSender.sendMessage("Only players can run this command!");
            return;
        }

        CorePlayer player = (CorePlayer) coreCommandSender;

        if (!player.hasPermission("networkjoinmessages.fakemessage")) {
            player.sendMessage(ConfigManager.getPluginConfig().getString("Messages.Commands.NoPermission"));
            return;
        }

        if (args.length < 1) {
            String msg = ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.NoArgument");
            player.sendMessage(msg);
            return;
        }

        String message;

        switch (args[0].toLowerCase()) {
            case "fakejoin":
            case "fj":
                message = MessageHandler.getInstance()
                        .formatJoinMessage(player);
                MessageHandler.getInstance()
                        .broadcastMessage(
                                HexChat.translateHexCodes(message),
                                "join",
                                player
                        );
                return;
            case "fakequit":
            case "fq":
                message = MessageHandler.getInstance()
                        .formatQuitMessage(player);
                MessageHandler.getInstance()
                        .broadcastMessage(
                                HexChat.translateHexCodes(message),
                                "leave",
                                player
                        );
                return;
            case "fakeswitch":
            case "fs":
                if (args.length < 3) {
                    String msg = ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.FakeSwitchNoArgument");
                    player.sendMessage(msg);
                    return;
                } else {
                    String fromName = args[1];
                    String toName = args[2];

                    message = MessageHandler.getInstance()
                            .formatSwitchMessage(player, fromName, toName);

                    MessageHandler.getInstance()
                            .broadcastMessage(
                                    HexChat.translateHexCodes(message),
                                    "switch",
                                    fromName,
                                    toName
                            );
                    return;
                }
            case "toggle":
                String msg = "";
                if (!player.hasPermission("networkjoinmessages.silent")) {
                    msg = ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.ToggleSilentNoPerm");
                    player.sendMessage(HexChat.translateHexCodes(msg));
                    return;
                }
                boolean state = !Storage.getInstance()
                        .getAdminMessageState(player);
                msg = ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.ToggleSilent");
                msg = msg.replace("<state>", state + "");
                player.sendMessage(HexChat.translateHexCodes(msg));
                Storage.getInstance().setAdminMessageState(player, state);
        }
    }

    @Override
    public String getRequiredPermssion() {
        return "networkjoinmessages.fakemessage";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        List<String> commandArguments = ImmutableList.of(
                "fakejoin",
                "fakequit",
                "fakeswitch",
                "fj",
                "fq",
                "fs",
                "toggle"
        );
        switch (args.length) {
            case 1:
                return commandArguments;
            case 2:
                if (args[0].equalsIgnoreCase("fs") || args[0].equalsIgnoreCase("fakeswitch")) {
                    return MessageHandler.getInstance().getServerNames();
                }
            case 3:
                if (args[0].equalsIgnoreCase("fs") || args[0].equalsIgnoreCase("fakeswitch")) {
                    return MessageHandler.getInstance().getServerNames();
                }
            default:
                return ImmutableList.of(
                        ConfigManager.getPluginConfig().getString("Messages.Commands.NoMoreArgumentsNeeded")
                );
        }
    }

}
