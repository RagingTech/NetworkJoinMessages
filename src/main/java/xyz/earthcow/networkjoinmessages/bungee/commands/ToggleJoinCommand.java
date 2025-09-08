package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.bungee.general.BungeeMain;
import xyz.earthcow.networkjoinmessages.common.general.Core;

public class ToggleJoinCommand extends Command implements TabExecutor {

    public ToggleJoinCommand() {
        super("networkjoinmessage", Core.getInstance().coreToggleJoinCommand.getRequiredPermission(), "njointoggle");
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        Core.getInstance().coreToggleJoinCommand.execute(
            commandSender instanceof ProxiedPlayer ?
                BungeeMain.getInstance().getOrCreatePlayer(((ProxiedPlayer) commandSender).getUniqueId())
                :
                new BungeeCommandSender(commandSender),
            args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return Core.getInstance().coreToggleJoinCommand.getTabCompletion(
            sender instanceof ProxiedPlayer ?
                BungeeMain.getInstance().getOrCreatePlayer(((ProxiedPlayer) sender).getUniqueId())
                :
                new BungeeCommandSender(sender)
            , args);
    }
}
