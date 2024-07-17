package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeCommandSender;
import xyz.earthcow.networkjoinmessages.common.commands.CoreFakeCommand;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

public class FakeCommand extends Command implements TabExecutor {

    private final CoreFakeCommand coreFakeCommand = NetworkJoinMessagesCore.getInstance().coreFakeCommand;

    public FakeCommand() {
        super("fakemessage", NetworkJoinMessagesCore.getInstance().coreFakeCommand.getRequiredPermssion(), "fm");
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        coreFakeCommand.execute(new BungeeCommandSender(commandSender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return coreFakeCommand.getTabCompletion(new BungeeCommandSender(sender), args);
    }
}
