package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.earthcow.networkjoinmessages.common.commands.CoreToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.velocity.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;

import java.util.List;

public class ToggleJoinCommand implements SimpleCommand {

    private final CoreToggleJoinCommand coreToggleJoinCommand;

    public ToggleJoinCommand(CoreToggleJoinCommand coreToggleJoinCommand) {
        this.coreToggleJoinCommand = coreToggleJoinCommand;
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        coreToggleJoinCommand.execute(
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
            .hasPermission(coreToggleJoinCommand.getRequiredPermission());
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        return coreToggleJoinCommand.getTabCompletion(
            invocation.source() instanceof Player ?
                VelocityMain.getInstance().getOrCreatePlayer(((Player) invocation.source()).getUniqueId())
                :
                new VelocityCommandSender(invocation.source()),
            invocation.arguments());
    }
}
