package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.velocity.abstaction.VelocityCommandSender;

import java.util.List;

public class ToggleJoinCommand implements SimpleCommand {

    private final CoreToggleJoinCommand coreToggleJoinCommand = new CoreToggleJoinCommand();

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        coreToggleJoinCommand.execute(new VelocityCommandSender(invocation.source()), invocation.arguments());
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation
            .source()
            .hasPermission(coreToggleJoinCommand.getRequiredPermssion());
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        return coreToggleJoinCommand.getTabCompletion(new VelocityCommandSender(invocation.source()), invocation.arguments());
    }
}
