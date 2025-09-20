package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import xyz.earthcow.networkjoinmessages.bungee.BungeeMain;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.common.commands.CoreImportCommand;

public class ImportCommand extends Command {

    private final CoreImportCommand coreImportCommand;

    public ImportCommand(CoreImportCommand coreImportCommand) {
        super("njoinimport", coreImportCommand.getRequiredPermission());
        this.coreImportCommand = coreImportCommand;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        coreImportCommand.execute(
            sender instanceof ProxiedPlayer ?
                BungeeMain.getInstance().getOrCreatePlayer(((ProxiedPlayer) sender).getUniqueId())
                :
                new BungeeCommandSender(sender)
            , args);
    }

}
