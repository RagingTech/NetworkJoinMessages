package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super("networkjoinreload", NetworkJoinMessagesCore.getInstance().coreReloadCommand.getRequiredPermission(), "njoinreload");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        NetworkJoinMessagesCore.getInstance().coreReloadCommand.execute(new BungeeCommandSender(sender), args);
    }
}
