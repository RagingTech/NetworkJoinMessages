package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityPlayer;

public class ImportCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        NetworkJoinMessagesCore.getInstance().coreImportCommand.execute(
            invocation.source() instanceof Player ?
                // If the CommandSource is a Player
                new VelocityPlayer((Player) invocation.source())
                :
                // If the CommandSource is not a Player
                new VelocityCommandSender(invocation.source()),
            invocation.arguments());
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation
            .source()
            .hasPermission(NetworkJoinMessagesCore.getInstance().coreImportCommand.getRequiredPermission());
    }
}
