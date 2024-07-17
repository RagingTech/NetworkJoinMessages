package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreReloadCommand;
import xyz.earthcow.networkjoinmessages.velocity.abstaction.VelocityCommandSender;

public class ReloadCommand implements SimpleCommand {

    private final CoreReloadCommand coreReloadCommand = new CoreReloadCommand();

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        coreReloadCommand.execute(new VelocityCommandSender(invocation.source()), invocation.arguments());
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation
            .source()
            .hasPermission(coreReloadCommand.getRequiredPermssion());
    }
}
