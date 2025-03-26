package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

public class ToggleJoinCommand extends Command implements TabExecutor {

    public ToggleJoinCommand() {
        super("networkjoinmessage", NetworkJoinMessagesCore.getInstance().coreToggleJoinCommand.getRequiredPermission(), "njointoggle");
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        NetworkJoinMessagesCore.getInstance().coreToggleJoinCommand.execute(new BungeeCommandSender(commandSender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return NetworkJoinMessagesCore.getInstance().coreToggleJoinCommand.getTabCompletion(new BungeeCommandSender(sender), args);
    }
}
