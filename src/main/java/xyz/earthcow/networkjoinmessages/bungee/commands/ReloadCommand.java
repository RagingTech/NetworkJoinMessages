package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import xyz.earthcow.networkjoinmessages.bungee.BungeeMain;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.common.commands.CoreReloadCommand;

public class ReloadCommand extends Command {

    private final CoreReloadCommand coreReloadCommand;

    public ReloadCommand(CoreReloadCommand coreReloadCommand) {
        super("networkjoinreload", coreReloadCommand.getRequiredPermission(), "njoinreload");
        this.coreReloadCommand = coreReloadCommand;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        coreReloadCommand.execute(
            sender instanceof ProxiedPlayer ?
                BungeeMain.getInstance().getOrCreatePlayer(((ProxiedPlayer) sender).getUniqueId())
                :
                new BungeeCommandSender(sender)
            , args);
    }
}
