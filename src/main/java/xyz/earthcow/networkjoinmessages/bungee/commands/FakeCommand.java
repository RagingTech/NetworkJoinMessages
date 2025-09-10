package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.bungee.BungeeMain;
import xyz.earthcow.networkjoinmessages.common.commands.CoreFakeCommand;
import xyz.earthcow.networkjoinmessages.common.Core;

public class FakeCommand extends Command implements TabExecutor {

    private final CoreFakeCommand coreFakeCommand = Core.getInstance().coreFakeCommand;

    public FakeCommand() {
        super("fakemessage", Core.getInstance().coreFakeCommand.getRequiredPermission(), "fm");
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        coreFakeCommand.execute(
            commandSender instanceof ProxiedPlayer ?
                BungeeMain.getInstance().getOrCreatePlayer(((ProxiedPlayer) commandSender).getUniqueId())
                :
                new BungeeCommandSender(commandSender),
            args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return coreFakeCommand.getTabCompletion(
            sender instanceof ProxiedPlayer ?
                BungeeMain.getInstance().getOrCreatePlayer(((ProxiedPlayer) sender).getUniqueId())
                :
                new BungeeCommandSender(sender)
            , args);
    }
}
