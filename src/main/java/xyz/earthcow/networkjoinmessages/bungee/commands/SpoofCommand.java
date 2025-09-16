package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import xyz.earthcow.networkjoinmessages.bungee.BungeeMain;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.common.commands.CoreSpoofCommand;

public class SpoofCommand extends Command implements TabExecutor {

    private final CoreSpoofCommand coreSpoofCommand;

    public SpoofCommand(CoreSpoofCommand coreSpoofCommand) {
        super("njoinspoof", coreSpoofCommand.getRequiredPermission());
        this.coreSpoofCommand = coreSpoofCommand;
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        coreSpoofCommand.execute(
            commandSender instanceof ProxiedPlayer ?
                BungeeMain.getInstance().getOrCreatePlayer(((ProxiedPlayer) commandSender).getUniqueId())
                :
                new BungeeCommandSender(commandSender),
            args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return coreSpoofCommand.getTabCompletion(
            sender instanceof ProxiedPlayer ?
                BungeeMain.getInstance().getOrCreatePlayer(((ProxiedPlayer) sender).getUniqueId())
                :
                new BungeeCommandSender(sender)
            , args);
    }
}
