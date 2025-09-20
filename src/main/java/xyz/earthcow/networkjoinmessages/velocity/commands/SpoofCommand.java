package xyz.earthcow.networkjoinmessages.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.earthcow.networkjoinmessages.common.commands.CoreSpoofCommand;
import xyz.earthcow.networkjoinmessages.velocity.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityCommandSender;

import java.util.List;

public class SpoofCommand implements SimpleCommand {

    private final CoreSpoofCommand coreSpoofCommand;

    public SpoofCommand(CoreSpoofCommand coreSpoofCommand) {
        this.coreSpoofCommand = coreSpoofCommand;
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        coreSpoofCommand.execute(
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
            .hasPermission(coreSpoofCommand.getRequiredPermission());
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        return coreSpoofCommand.getTabCompletion(
            invocation.source() instanceof Player ?
                VelocityMain.getInstance().getOrCreatePlayer(((Player) invocation.source()).getUniqueId())
                :
                new VelocityCommandSender(invocation.source()),
            invocation.arguments());
    }
}
