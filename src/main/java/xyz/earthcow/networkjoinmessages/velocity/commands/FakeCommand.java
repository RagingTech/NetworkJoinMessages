package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.earthcow.networkjoinmessages.common.commands.CoreFakeCommand;
import xyz.earthcow.networkjoinmessages.common.general.Core;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;

import java.util.List;

public class FakeCommand implements SimpleCommand {

    private final CoreFakeCommand coreFakeCommand = Core.getInstance().coreFakeCommand;

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        coreFakeCommand.execute(
            invocation.source() instanceof Player ?
                // If the CommandSource is a Player
                VelocityMain.getInstance().getOrCreatePlayer(((Player) invocation.source()).getUniqueId())
                :
                // If the CommandSource is not a Player
                new VelocityCommandSender(invocation.source()),
            invocation.arguments());
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation
            .source()
            .hasPermission(coreFakeCommand.getRequiredPermission());
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        return coreFakeCommand.getTabCompletion(
            invocation.source() instanceof Player ?
                VelocityMain.getInstance().getOrCreatePlayer(((Player) invocation.source()).getUniqueId())
                :
                new VelocityCommandSender(invocation.source()),
            invocation.arguments());
    }
}
