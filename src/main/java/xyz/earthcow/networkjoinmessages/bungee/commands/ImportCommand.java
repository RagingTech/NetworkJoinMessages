package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeePlayer;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

public class ImportCommand extends Command {

    public ImportCommand() {
        super("njoinimport", NetworkJoinMessagesCore.getInstance().coreImportCommand.getRequiredPermission());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        NetworkJoinMessagesCore.getInstance().coreImportCommand.execute(
            sender instanceof ProxiedPlayer ?
                new BungeePlayer((ProxiedPlayer) sender)
                :
                new BungeeCommandSender(sender)
            , args);
    }

}
