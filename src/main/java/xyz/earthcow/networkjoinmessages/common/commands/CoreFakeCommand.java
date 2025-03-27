package xyz.earthcow.networkjoinmessages.common.commands;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
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
            player.sendMessage(MessageHandler.getInstance().formatMessage(
                ConfigManager.getPluginConfig().getString("Messages.Commands.NoPermission"),
                player
            ));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(MessageHandler.getInstance().formatMessage(
                ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.NoArgument"),
                player
            ));
            return;
        }

        Component message;

        switch (args[0].toLowerCase()) {
            case "fakejoin":
            case "fj":
                message = MessageHandler.getInstance().formatJoinMessage(player);
                MessageHandler.getInstance().broadcastMessage(message, "join", player);
                return;
            case "fakequit":
            case "fq":
                message = MessageHandler.getInstance().formatQuitMessage(player);
                MessageHandler.getInstance().broadcastMessage(message, "leave", player);
                return;
            case "fakeswitch":
            case "fs":
                if (args.length < 3) {
                    player.sendMessage(MessageHandler.getInstance().formatMessage(
                        ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.FakeSwitchNoArgument"),
                        player
                    ));
                    return;
                }
                String fromName = args[1];
                String toName = args[2];

                message = MessageHandler.getInstance().formatSwitchMessage(player, fromName, toName);
                MessageHandler.getInstance().broadcastMessage(message, "switch", fromName, toName);
                return;
            case "toggle":
                if (!player.hasPermission("networkjoinmessages.silent")) {
                    player.sendMessage(MessageHandler.getInstance().formatMessage(
                        ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.ToggleSilentNoPerm"),
                        player
                    ));
                    return;
                }
                boolean state = !Storage.getInstance().getAdminMessageState(player);
                Component toggleMessage = MessageHandler.getInstance().formatMessage(
                    ConfigManager.getPluginConfig().getString("Messages.Commands.Fakemessage.ToggleSilent"),
                    player
                ).replaceText(builder -> builder
                    .matchLiteral("<state>")
                    .replacement(String.valueOf(state))
                );
                player.sendMessage(toggleMessage);
                Storage.getInstance().setAdminMessageState(player, state);
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
                    return MessageHandler.getInstance().getServerNames();
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