package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreFakeCommand;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;

import java.util.List;

public class FakeCommand implements SimpleCommand {

    private final CoreFakeCommand coreFakeCommand = NetworkJoinMessagesCore.getInstance().coreFakeCommand;

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        coreFakeCommand.execute(new VelocityCommandSender(invocation.source()), invocation.arguments());
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation
            .source()
            .hasPermission(coreFakeCommand.getRequiredPermission());
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        return coreFakeCommand.getTabCompletion(new VelocityCommandSender(invocation.source()), invocation.arguments());
    }
}
