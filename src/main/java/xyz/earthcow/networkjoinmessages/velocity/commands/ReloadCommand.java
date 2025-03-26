package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityPlayer;

public class ReloadCommand implements SimpleCommand {

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        NetworkJoinMessagesCore.getInstance().coreReloadCommand.execute(
            invocation.source() instanceof Player ?
                // If the CommandSource is a Player
                new VelocityPlayer((Player) invocation.source())
                :
                // If the CommandSource is not a Player
                new VelocityCommandSender(invocation.source()),
            invocation.arguments());
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation
            .source()
            .hasPermission(NetworkJoinMessagesCore.getInstance().coreReloadCommand.getRequiredPermission());
    }
}
