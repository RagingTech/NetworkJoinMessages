package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.bungee.BungeeMain;
import xyz.earthcow.networkjoinmessages.common.Core;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super("networkjoinreload", Core.getInstance().coreReloadCommand.getRequiredPermission(), "njoinreload");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Core.getInstance().coreReloadCommand.execute(
            sender instanceof ProxiedPlayer ?
                BungeeMain.getInstance().getOrCreatePlayer(((ProxiedPlayer) sender).getUniqueId())
                :
                new BungeeCommandSender(sender)
            , args);
    }
}
