package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;

public class ReloadCommand implements SimpleCommand {

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        NetworkJoinMessagesCore.getInstance().coreReloadCommand.execute(new VelocityCommandSender(invocation.source()), invocation.arguments());
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation
            .source()
            .hasPermission(NetworkJoinMessagesCore.getInstance().coreReloadCommand.getRequiredPermssion());
    }
}
