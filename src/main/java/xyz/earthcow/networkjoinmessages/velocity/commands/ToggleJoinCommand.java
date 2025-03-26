package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.earthcow.networkjoinmessages.common.commands.CoreToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityPlayer;

import java.util.List;

public class ToggleJoinCommand implements SimpleCommand {

    private final CoreToggleJoinCommand coreToggleJoinCommand = NetworkJoinMessagesCore.getInstance().coreToggleJoinCommand;

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        coreToggleJoinCommand.execute(
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
            .hasPermission(coreToggleJoinCommand.getRequiredPermission());
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        return coreToggleJoinCommand.getTabCompletion(
            invocation.source() instanceof Player ?
                new VelocityPlayer((Player) invocation.source())
                :
                new VelocityCommandSender(invocation.source()),
            invocation.arguments());
    }
}
